package com.greatnowhere.iradar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cobra.iradar.messaging.CobraMessageThreat;
import com.cobra.iradar.protocol.RadarMessageAlert;

/**
 * Manages currently active threats and displays them
 * @author pzeltins
 *
 */
public class ThreatManager {

	/**
	 * Threats currently displayed
	 */
	public static List<Threat> activeThreats = new ArrayList<Threat>();
	
	private static WindowManager wm = null;
	private static WindowManager.LayoutParams params;
	private static AudioManager am = null;
	private static SoundPool alertSounds = null;
	private static Map<String, Integer> alertSoundsLoaded = new HashMap<String, Integer>();
	private static int originalVolume = 0;
	private static View mainThreatView;
	private static LinearLayout mainThreatLayout; 
	private static boolean isThreatActive = false;
	
	private static Context ctx;
	
	public static void init(Context appContext) {
		ctx = appContext;
	    params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER | Gravity.TOP;
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        
        // Inflate main threats layout
        mainThreatView = View.inflate(appContext, R.layout.threats_view, null);
        mainThreatLayout = (LinearLayout) mainThreatView.findViewById(R.id.layoutThreats);
        mainThreatLayout.setLayoutTransition(new LayoutTransition());
        
        am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        originalVolume = am.getStreamVolume(AudioManager.STREAM_ALARM);

        alertSounds = new SoundPool(3, AudioManager.STREAM_ALARM, 0);
        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_KA, alertSounds.load(ctx, R.raw.ka1, 1));
        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_HAZARD, alertSounds.load(ctx, R.raw.ev, 1));
        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_K, alertSounds.load(ctx, R.raw.k1, 1));
        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_KU, alertSounds.load(ctx, R.raw.ku1, 1));
        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_LASER, alertSounds.load(ctx, R.raw.laser, 1));
        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_POP, alertSounds.load(ctx, R.raw.ka1, 1));
        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_RDD, alertSounds.load(ctx, R.raw.vg2, 1));
        alertSoundsLoaded.put(RadarMessageAlert.ALERT_SOUND_X, alertSounds.load(ctx, R.raw.x1, 1));
	}
	
	public static void newThreat(CobraMessageThreat alert) {
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
		if ( Preferences.isSetAlertLevel() ) {
			// Restore alarms volume
			am.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
		}
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
	
	private static int getThreatColor(int strength) {
		// strength is assumed to be 0 - 5 (5=max)
		return Color.argb(240, 150 + (strength * 20), 0, 0);
	}
	
	private static float getThreatSoundPitch(int strength) {
		return (((float) strength - 1) / 8f) + 1f;
	}

	/**
	 * Active threat class
	 * @author pzeltins
	 *
	 */
	private static class Threat {
		/**
		 * View displaying current threat
		 */
		private View view;
		private CobraMessageThreat alert;
		private int soundStreamId;
		/**
		 * True if this threat has been added to the view
		 */
		private boolean isShowing = false;
		
		private Threat(View v, CobraMessageThreat a) {
			view = v;
			alert = a;
		}

		private void showThreat() {
			if ( view == null || alert == null )
				return;
			updateThreat(alert.strength);
		}
		
		private void removeThreat() {
			mainThreatLayout.removeView(view);
			isShowing = false;
			if ( soundStreamId != 0 ) {
				alertSounds.stop(soundStreamId);
				soundStreamId = 0;
			}
		}
		
		private void playAlert() {
			if ( Preferences.isSetAlertLevel() ) {
				am.setStreamVolume(AudioManager.STREAM_ALARM, Preferences.getAlertLevel(), 0);
			}
			// start looping play
			soundStreamId = alertSounds.play(alertSoundsLoaded.get(alert.alertType.getSound()), 1, 1, 1, 1, 
					getThreatSoundPitch(alert.strength));
		}
		
		private void updateThreat(CobraMessageThreat t) {
			this.alert.frequency = t.frequency;
			this.alert.alertType = t.alertType;
			updateThreat(t.strength);
		}
		
		private void updateThreat(int newStrength) {
			if ( view == null || alert == null )
				return;
			// only update if strength changes
			if ( newStrength != alert.strength || !isShowing ) {
				// stop any alert sound currently playing
				if ( soundStreamId != 0 ) {
					alertSounds.stop(soundStreamId);
					soundStreamId = 0;
				}
				if ( !isShowing ) {
					mainThreatLayout.addView(view);
					isShowing = true;
				}
				TextView band = (TextView) view.findViewById(R.id.textViewBand);
				TextView freq = (TextView) view.findViewById(R.id.textViewFrequency);
				band.setText(alert.alertType.getName());
				freq.setText(Float.toString(alert.frequency) + " Ghz");
				band.setTextColor(ColorStateList.valueOf(getThreatColor(alert.strength)));
				playAlert();
			}
		}
		
	}


}
