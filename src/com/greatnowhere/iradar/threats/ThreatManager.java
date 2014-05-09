package com.greatnowhere.iradar.threats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.animation.LayoutTransition;
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
import com.greatnowhere.iradar.R;
import com.greatnowhere.iradar.config.Preferences;
import com.greatnowhere.iradar.location.LocationManager;

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
	private static View mainThreatView;
	static LinearLayout mainThreatLayout; 
	private static boolean isThreatActive = false;
	
	private static Context ctx;
	
	public static void init(Context appContext) {
		ctx = appContext;
	    params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                ( Preferences.isTurnScreenOnForAlerts() ? WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | 
                		WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                		WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD : 0),
                PixelFormat.TRANSLUCENT);
	    
        params.gravity = Gravity.CENTER | Gravity.TOP;
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        
        // Inflate main threats layout
        mainThreatView = View.inflate(appContext, R.layout.threats_view, null);
        mainThreatLayout = (LinearLayout) mainThreatView.findViewById(R.id.layoutThreats);
        mainThreatLayout.setLayoutTransition(new LayoutTransition());
        
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
	
	public static void stop() {
		removeThreats();
		LocationManager.stop();
	}
	
	public static void newThreat(CobraMessageThreat alert) {
		// ignore frequency 0, must be a fluke
		if ( alert.frequency == 0f )
			return;
		
		Threat t = findExistingThreat( alert );
		if ( t == null ) {
			View v = View.inflate(ctx, R.layout.threat, null);
			t = new Threat(v, alert);
			t.showThreat();
			activeThreats.add(t);
		} else {
			t.updateThreat(alert);
		}
		if ( !isThreatActive ) {
			wm.addView(mainThreatView, params);
		}
		isThreatActive = true;
	}
	
	public static void removeThreats() {
		if ( activeThreats.size() == 0 ) 
			return;
		
		for ( Threat t : activeThreats ) {
			t.removeThreat();
		}
		activeThreats = new ArrayList<Threat>();
		AlertAudioManager.restoreOldAlertVolume();
		isThreatActive = false;
		wm.removeView(mainThreatView);

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


}
