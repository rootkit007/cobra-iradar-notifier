package com.greatnowhere.radar.threats;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff.Mode;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.greatnowhere.radar.R;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.location.RadarLocationManager;
import com.greatnowhere.radar.messaging.RadarMessageThreat;
import com.greatnowhere.radar.threats.ThreatManager.ThreatCredibility;

/**
 * Active threat class
 * @author pzeltins
 *
 */
public class Threat {
	
	private static final String TAG = Threat.class.getCanonicalName();
	
	public static final boolean AUDIBLE_FALSE_THREATS = false;
	public static final float AUTOMUTE_VOLUME_PCT = 0.3f;
	
	/**
	 * View displaying current threat
	 */
	private View view;
	protected RadarMessageThreat alert;
	private int soundStreamId;
	private TextView band;
	private TextView freq;
	private ProgressBar strength;
	/**
	 * Max strength of this threat
	 */
	protected int maxStrength = 0;
	protected Set<Location> locations;
	protected Long startTimeMillis = System.currentTimeMillis();
	protected Long endTimeMillis;
	protected ThreatManager.ThreatCredibility credibility = ThreatCredibility.LEGIT;
	protected AtomicBoolean isThreatAudibleNow = new AtomicBoolean(false);
	protected Timer autoMuteTimer;
	/**
	 * Relative volume of this threat, used for automuting
	 */
	protected float volume = 1f;
	
	/**
	 * True if this threat has been added to the view
	 */
	private boolean isShowing = false;
	
	public Threat() {
		
	}
	
	public Threat(View v, RadarMessageThreat a, ThreatManager.ThreatCredibility cred) {
		view = v;
		alert = a;
		credibility = cred;
		band = (TextView) view.findViewById(R.id.textViewBand);
		freq = (TextView) view.findViewById(R.id.textViewFrequency);
		strength = (ProgressBar) view.findViewById(R.id.threatViewStrength);
	}

	void showThreat() {
		if ( view == null || alert == null )
			return;
		
		updateThreat(alert.strength, credibility);
	}
	
	void removeThreat() {
		Log.i(TAG,"removeThreat");
		endTimeMillis = System.currentTimeMillis();
		if ( isShowing )
			hideThreat();
		isShowing = false;
		if ( soundStreamId != 0 ) {
			Log.i(TAG,"silence " + toString());
			ThreatManager.alertSounds.stop(soundStreamId);
			soundStreamId = 0;
			AlertAudioManager.restoreOldAlertVolume();
			isThreatAudibleNow.set(false);
		}
		if ( Preferences.isLogThreats() ) {
			ThreatLogger.logThreat(this);
		}
	}
	
	private void hideThreat() {
		ThreatManager.post(new Runnable() {
			public void run() {
				ThreatManager.instance.mainThreatLayout.removeView(view);
				isShowing = false;
			}
		});
	}
	
	private synchronized void playAlert() {
		Log.i(TAG,"playAlert " + toString());
		if ( !isThreatAudibleNow.get() ) {
			isThreatAudibleNow.set(true);
			AlertAudioManager.setOurAlertVolume();
			// start automute timer
			if ( autoMuteTimer != null )
				autoMuteTimer.cancel();
			autoMuteTimer = new Timer(true);
			autoMuteTimer.schedule(new AutoMuteTask(), Preferences.getAlertAutoMuteDelay() * 1000L);
		}
		if ( Preferences.isAutoMuteImmediatelyDuringCalls() && ThreatManager.isPhoneCallActive() ) {
			volume = AUTOMUTE_VOLUME_PCT;
		}
		if ( soundStreamId != 0 ) 
			ThreatManager.alertSounds.stop(soundStreamId);
		// start looping play
		soundStreamId = ThreatManager.alertSounds.play(ThreatManager.alertSoundsLoaded.get(alert.alertType.getSound()), volume, volume, 1, -1, 
				ThreatManager.getThreatSoundPitch(alert.strength));
	}
	
	void updateThreat(RadarMessageThreat t, ThreatCredibility cred) {
		this.alert.frequency = t.frequency;
		this.alert.alertType = t.alertType;
		updateThreat(t.strength, cred);
	}
	
	private void updateThreat(int newStrength, ThreatCredibility cred) {
		if ( view == null || alert == null )
			return;
		recordLocation();
		maxStrength = Math.max(maxStrength, newStrength);
		// only update if strength or credibility changes
		if ( newStrength != alert.strength || credibility != cred || !isShowing ) {
			
			this.alert.strength = newStrength;
			this.credibility = cred;
			if ( isShowVisibleThreat() ) {
				ThreatManager.post(new Runnable() {
					public void run() {
						if ( !isShowing ) {
							ThreatManager.instance.mainThreatLayout.addView(view);
							isShowing = true;
						}
						band.setText(alert.alertType.getName());
						freq.setText(Float.toString(alert.frequency) + " Ghz");
						int color = ThreatManager.getThreatColor(alert.strength, credibility);
						ColorStateList threatColor = ColorStateList.valueOf(color);
						band.setTextColor(threatColor);
						freq.setTextColor(threatColor);
						strength.setProgress(alert.strength);
						strength.getProgressDrawable().getCurrent().setColorFilter(color, Mode.MULTIPLY);
					}
				});
			} else {
				if ( isShowing ) {
					hideThreat();
				}
			}
			if ( isPlayAudibleThreat() )
				playAlert();
		}
	}
	
	private void recordLocation() {
		if ( Preferences.isLogThreatLocation() && RadarLocationManager.isReady() ) {
			if ( locations == null )
				locations = new LinkedHashSet<Location>();
			locations.add(RadarLocationManager.getCurrentLoc());
		}
	}
	
	/**
	 * Returns true if this threat should be made audible 
	 * @return
	 */
	public boolean isPlayAudibleThreat() {
		return ( credibility != ThreatCredibility.FAKE && credibility != ThreatCredibility.HIDDEN) ||
				( AUDIBLE_FALSE_THREATS );
	}

	/**
	 * Returns true if this threat should be made visible
	 * @return
	 */
	public boolean isShowVisibleThreat() {
		return isShowVisibleThreat(this.credibility);
	}
	
	public static boolean isShowVisibleThreat(ThreatCredibility c) {
		return ( c != ThreatCredibility.FAKE && c != ThreatCredibility.HIDDEN) ||
				( Preferences.isShowHiddenThreats() );
	}
	
	/**
	 * Human-readable one-line threat description
	 */
	@Override
	public String toString() {
		return "Threat " + alert.alertType.getName() + " " + alert.frequency + " (" + credibility.getName() + ")"; 
	}

	private class AutoMuteTask extends TimerTask {
		@Override
		public void run() {
			Log.i(TAG,"Automuted threat " + toString());
			volume = AUTOMUTE_VOLUME_PCT;
			playAlert();
		}
	}


}