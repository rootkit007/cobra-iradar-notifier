package com.greatnowhere.radar.threats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.SoundPool;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.cobra.iradar.protocol.CobraRadarMessageAlert;
import com.greatnowhere.radar.R;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.location.RadarLocationManager;
import com.greatnowhere.radar.messaging.RadarMessageNotification;
import com.greatnowhere.radar.messaging.RadarMessageThreat;

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
	private static Threat currentThreat;
	private static AtomicBoolean wasScreenOn = new AtomicBoolean(false);
	private Timer autoMuteTimer;
	private static TelephonyManager tm;
	
	private static Context ctx;
	
	public static void init(Context appContext) {
		if ( ctx != null )
			return;
		
		instance = new ThreatManager();
		eventBus = EventBus.getDefault();
		if ( !eventBus.isRegistered(instance))
			eventBus.register(instance);
		
		ctx = appContext;
		
		tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
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
		        params.dimAmount = 0.4f;
		        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		        
		        instance.mainThreatLayout = (LinearLayout) instance.mainThreatView.findViewById(R.id.layoutThreats);
		        
		        alertSounds = new SoundPool(3, AlertAudioManager.OUTPUT_STREAM, 0);
		        alertSoundsLoaded.put(CobraRadarMessageAlert.ALERT_SOUND_KA, alertSounds.load(ctx, R.raw.ka1, 1));
		        alertSoundsLoaded.put(CobraRadarMessageAlert.ALERT_SOUND_HAZARD, alertSounds.load(ctx, R.raw.ev, 1));
		        alertSoundsLoaded.put(CobraRadarMessageAlert.ALERT_SOUND_K, alertSounds.load(ctx, R.raw.k1, 1));
		        alertSoundsLoaded.put(CobraRadarMessageAlert.ALERT_SOUND_KU, alertSounds.load(ctx, R.raw.ku1, 1));
		        alertSoundsLoaded.put(CobraRadarMessageAlert.ALERT_SOUND_LASER, alertSounds.load(ctx, R.raw.laser, 1));
		        alertSoundsLoaded.put(CobraRadarMessageAlert.ALERT_SOUND_POP, alertSounds.load(ctx, R.raw.ka1, 1));
		        alertSoundsLoaded.put(CobraRadarMessageAlert.ALERT_SOUND_RDD, alertSounds.load(ctx, R.raw.vg2, 1));
		        alertSoundsLoaded.put(CobraRadarMessageAlert.ALERT_SOUND_X, alertSounds.load(ctx, R.raw.x1, 1));
		        
		        // Initialize location manager
		        RadarLocationManager.init(ctx);
		        ThreatLogger.init(ctx);
			}
		}));
	}
	
	/**
	 * Returns human-readable last alert string
	 * @return
	 */
	public static String getCurrentThreat() {
		return ( currentThreat == null ? "" : currentThreat.toString() );
	}
	
	public static void stop() {
		removeThreats();
		RadarLocationManager.stop();
	}
	
	public static ThreatCredibility newThreat(RadarMessageThreat alert) {
		if ( alert.isVolumeChangeMessage() )
			return ThreatCredibility.FAKE;

		ThreatCredibility cred = ThreatCredibility.LEGIT;
		
		// check if fake
		if ( Preferences.isFakeAlertDetection() && Preferences.isLogThreatLocation() && RadarLocationManager.isReady() ) {
			int countSimilar = ThreatLogger.countSimilarThreatOccurences(alert, RadarLocationManager.getCurrentLoc(), Preferences.getFakeAlertDetectionRadius());
			if ( countSimilar > Preferences.getFakeAlertOccurenceThreshold() ) {
				cred = ThreatCredibility.FAKE;
			}
			if ( countSimilar > 1 && countSimilar <= Preferences.getFakeAlertOccurenceThreshold() ) {
				cred = ThreatCredibility.SUSPECT_FAKE;
			}
		}
		
		// check if we are above min required speed
		if ( Preferences.getThreatShowMinSpeed() > 0 && RadarLocationManager.isReady() ) {
			if ( RadarLocationManager.getCurrentSpeedKph() <= Preferences.getThreatShowMinSpeed() ) {
				cred = ThreatCredibility.HIDDEN;
			}
		}
		
		if ( !isThreatActive.get() && Threat.isShowVisibleThreat(cred) ) {
			showMainView();
		}
		Threat t = findExistingThreat( alert );
		if ( t == null ) {
			View v = View.inflate(ctx, R.layout.threat, null);
			t = new Threat(v, alert, cred);
			t.showThreat();
			activeThreats.add(t);
			addLogMessage(t.toString());
		} else {
			t.updateThreat(alert, cred);
		}
		currentThreat = t;
		
		return cred;
	}
	
	private synchronized static void showMainView() {
		isThreatActive.set(true);
		eventBus.post(new UIRunnableEvent(new Runnable() {
			public void run() {
				wm.addView(instance.mainThreatView, params);
			}
		}));
		if ( Preferences.getAlertAutoMuteDelay() > 0 ) {
			instance.autoMuteTimer = new Timer();
			instance.autoMuteTimer.schedule(instance.new AutoMuteTask(), (long) Preferences.getAlertAutoMuteDelay() * 1000L );
		}
	}
	
	public synchronized static void removeThreats() {
		
		currentThreat = null;
		
		if ( activeThreats.size() == 0 ) 
			return;
		
		for ( Threat t : activeThreats ) {
			addLogMessage("Ended threat " + t.toString());
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
		
		if ( wasScreenOn.get() ) {
			turnScreenOff();
		}

	}
	
	// TODO: add fake blank activity 
	private static void turnScreenOff() {
	}
	
	private static Threat findExistingThreat(RadarMessageThreat other) {
		for ( Threat t : activeThreats ) {
			if ( t.alert.equals(other) )    
				return t;
		}
		return null;
	}
	
	private ThreatManager() {
		super();
	}
	
	static int getThreatColor(int strength, ThreatCredibility cred) {
		// strength is assumed to be 0 - 5 (5=max)
		int red = ( cred == ThreatCredibility.LEGIT || cred == ThreatCredibility.SUSPECT_FAKE ? 155 + (strength * 20) : 10);
		int green = ( cred == ThreatCredibility.FAKE ? 155 + (strength * 20) : 10);
		int blue = ( cred == ThreatCredibility.HIDDEN ? 155 + (strength * 20) : 10);
		return Color.argb(255, red, green, blue);
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
	
	public static void addLogMessage(String s) {
		eventBus.post(new RadarMessageNotification(s));
	}
	
	public static boolean isPhoneCallActive() {
		return (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE);
	}
	
	private class AutoMuteTask extends TimerTask {
		@Override
		public void run() {
			AlertAudioManager.setAutoMuteVolume();
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
