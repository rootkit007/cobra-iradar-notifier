package com.greatnowhere.iradar.threats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.SoundPool;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.cobra.iradar.messaging.CobraMessageThreat;
import com.cobra.iradar.protocol.RadarMessageAlert;
import com.cobra.iradar.protocol.RadarMessageNotification;
import com.greatnowhere.iradar.R;
import com.greatnowhere.iradar.config.Preferences;
import com.greatnowhere.iradar.location.LocationManager;

import de.greenrobot.event.EventBus;

/**
 * Manages currently active threats and displays them
 * @author pzeltins
 *
 */
public class ThreatManager {

	/**
	 * Threats currently displayed
	 */
	private static List<Threat> activeThreats = new ArrayList<Threat>();
	
	private static WindowManager wm = null;
	private static WindowManager.LayoutParams params;
	static SoundPool alertSounds = null;
	static Map<String, Integer> alertSoundsLoaded = new HashMap<String, Integer>();
	View mainThreatView;
	LinearLayout mainThreatLayout;
	private static AtomicBoolean isThreatActive = new AtomicBoolean(false);
	private static EventBus eventBus;
	protected static ThreatManager instance;
	//private static WakeLock alertWakeLock = null;
	
	private static Context ctx;
	
	public static void init(Context appContext) {
		if ( ctx != null )
			return;
		
		instance = new ThreatManager();
		eventBus = EventBus.getDefault();
		if ( !eventBus.isRegistered(instance))
			eventBus.register(instance);
		
		ctx = appContext;
		
        // Inflate main threats layout
        instance.mainThreatView = View.inflate(ctx, R.layout.threats_view, null);
        
		eventBus.post(new UIRunnableEvent(new Runnable() {
			public void run() {
			    params = new WindowManager.LayoutParams(
		                WindowManager.LayoutParams.WRAP_CONTENT,
		                WindowManager.LayoutParams.WRAP_CONTENT,
		                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
		                	WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
		                	WindowManager.LayoutParams.FLAG_DIM_BEHIND |
		                	( Preferences.isTurnScreenOnForAlerts() ? WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | 
		                		WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
		                		WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD : 0),
		                PixelFormat.TRANSLUCENT);
			    
		        params.gravity = Gravity.CENTER | Gravity.TOP;
		        params.dimAmount = 0.3f;
		        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		        
		        instance.mainThreatLayout = (LinearLayout) instance.mainThreatView.findViewById(R.id.layoutThreats);
		        
		        alertSounds = new SoundPool(3, AlertAudioManager.OUTPUT_STREAM, 0);
		        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_KA, alertSounds.load(ctx, R.raw.ka1, 1));
		        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_HAZARD, alertSounds.load(ctx, R.raw.ev, 1));
		        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_K, alertSounds.load(ctx, R.raw.k1, 1));
		        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_KU, alertSounds.load(ctx, R.raw.ku1, 1));
		        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_LASER, alertSounds.load(ctx, R.raw.laser, 1));
		        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_POP, alertSounds.load(ctx, R.raw.ka1, 1));
		        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_RDD, alertSounds.load(ctx, R.raw.vg2, 1));
		        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_X, alertSounds.load(ctx, R.raw.x1, 1));
		        
		        // Initialize location manager
		        LocationManager.init(ctx);
		        ThreatLogger.init(ctx);
			}
		}));
	}
	
	public static void stop() {
		removeThreats();
		LocationManager.stop();
	}
	
	public static ThreatCredibility newThreat(CobraMessageThreat alert) {
		// ignore frequency 0, must be a fluke
		ThreatCredibility cred = ThreatCredibility.LEGIT;
		if ( alert.frequency == 0f )
			return ThreatCredibility.FAKE;
		
		// check if fake
		if ( Preferences.isFakeAlertDetection() && Preferences.isLogThreatLocation() && LocationManager.isReady() ) {
			int countSimilar = ThreatLogger.countSimilarThreatOccurences(alert, LocationManager.getCurrentLoc(), Preferences.getFakeAlertDetectionRadius());
			if ( countSimilar > Preferences.getFakeAlertOccurenceThreshold() ) {
				eventBus.post(new RadarMessageNotification("Fake alert detected " + alert.alertType.getName() + " " + alert.frequency));
				cred = ThreatCredibility.FAKE;
			}
			if ( countSimilar > 1 && countSimilar <= Preferences.getFakeAlertOccurenceThreshold() ) {
				eventBus.post(new RadarMessageNotification("Possible fake alert detected " + alert.alertType.getName() + " " + alert.frequency));
				cred = ThreatCredibility.SUSPECT_FAKE;
			}
		}
		
		// check if we are above min required speed
		if ( Preferences.getThreatShowMinSpeed() > 0 && LocationManager.isReady() ) {
			if ( LocationManager.getCurrentSpeedKph() <= Preferences.getThreatShowMinSpeed() ) {
				eventBus.post(new RadarMessageNotification("Hiding alert (speed below " + Preferences.getThreatShowMinSpeed()
						+ "kph) " + alert.alertType.getName() + " " + alert.frequency));
				cred = ThreatCredibility.HIDDEN;
			}
		}
		
		if ( !isThreatActive.get() && Threat.isShowVisibleThreat(cred) ) {
			isThreatActive.set(true);
			eventBus.post(new UIRunnableEvent(new Runnable() {
				public void run() {
					wm.addView(instance.mainThreatView, params);
				}
			}));
		}
		Threat t = findExistingThreat( alert );
		if ( t == null ) {
			View v = View.inflate(ctx, R.layout.threat, null);
			t = new Threat(v, alert, cred);
			t.showThreat();
			activeThreats.add(t);
		} else {
			t.updateThreat(alert);
		}
		
		return ThreatCredibility.LEGIT;
	}
	
	public synchronized static void removeThreats() {
		if ( activeThreats.size() == 0 ) 
			return;
		
		/*
		if ( alertWakeLock != null ) {
			alertWakeLock.release();
			alertWakeLock = null;
		}
		*/
		
		for ( Threat t : activeThreats ) {
			t.removeThreat();
		}
		activeThreats = new ArrayList<Threat>();
		AlertAudioManager.restoreOldAlertVolume();
		isThreatActive.set(false);
		eventBus.post(new UIRunnableEvent(new Runnable() {
			public void run() {
				instance.mainThreatLayout.removeAllViews();
				wm.removeView(instance.mainThreatView);
			}
		}));

	}
	
	private static Threat findExistingThreat(CobraMessageThreat other) {
		for ( Threat t : activeThreats ) {
			if ( t.alert.equals(other) )    
				return t;
		}
		return null;
	}
	
	private ThreatManager() {
		super();
	}
	
	static int getThreatColor(int strength) {
		// strength is assumed to be 0 - 5 (5=max)
		return Color.argb(255, 155 + (strength * 20), 10, 10);
	}
	
	static float getThreatSoundPitch(int strength) {
		return (((float) strength - 1) / 16f) + 1f;
	}

	/**
	 * Hacky helper class and event handlers for UI interaction
	 * @param event
	 */
	public void onEventMainThread(UIRunnableEvent event) {
		event.r.run();
	}
	
	protected static void post(Runnable r) {
		eventBus.post(new UIRunnableEvent(r));
	}
	
	public static class UIRunnableEvent {
		protected Runnable r;
		
		protected UIRunnableEvent(Runnable r) {
			this.r = r;
		}
	}

	/**
	 * Status returned by newThreat
	 * can be FAKE, POSSIBLE_FAKE, etc
	 * @author pzeltins
	 *
	 */
	public static enum ThreatCredibility {
		HIDDEN("Hidden",3),FAKE("Fake alert",2), SUSPECT_FAKE("Possible fake alert",1), LEGIT("Real Alert",0);
		
		private String name;
		private int code;

		public String getName() {
			return name;
		}
		
		public int getCode() {
			return code;
		}
		
		ThreatCredibility(String n, int c) {
			name = n;
			code = c;
		}
		
	}
}
