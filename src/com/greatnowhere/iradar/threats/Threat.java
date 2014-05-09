package com.greatnowhere.iradar.threats;

import java.util.LinkedHashSet;
import java.util.Set;

import android.content.res.ColorStateList;
import android.location.Location;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cobra.iradar.messaging.CobraMessageThreat;
import com.greatnowhere.iradar.R;
import com.greatnowhere.iradar.config.Preferences;
import com.greatnowhere.iradar.location.LocationManager;

/**
 * Active threat class
 * @author pzeltins
 *
 */
public class Threat {
	
	/**
	 * View displaying current threat
	 */
	private View view;
	protected CobraMessageThreat alert;
	private int soundStreamId;
	private TextView band;
	private TextView freq;
	private ProgressBar strength;
	protected Set<Location> locations;
	protected Long startTimeMillis = System.currentTimeMillis();
	
	/**
	 * True if this threat has been added to the view
	 */
	private boolean isShowing = false;
	
	public Threat() {
		
	}
	
	public Threat(View v, CobraMessageThreat a) {
		view = v;
		alert = a;
		band = (TextView) view.findViewById(R.id.textViewBand);
		freq = (TextView) view.findViewById(R.id.textViewFrequency);
		strength = (ProgressBar) view.findViewById(R.id.threatViewStrength);
	}

	void showThreat() {
		if ( view == null || alert == null )
			return;
		updateThreat(alert.strength);
	}
	
	void removeThreat() {
		ThreatManager.mainThreatLayout.removeView(view);
		isShowing = false;
		if ( soundStreamId != 0 ) {
			ThreatManager.alertSounds.stop(soundStreamId);
			soundStreamId = 0;
		}
		if ( Preferences.isLogThreats() ) {
			ThreatLogger.logThreat(this);
		}
	}
	
	private void playAlert() {
		AlertAudioManager.setOurAlertVolume();
		// start looping play
		soundStreamId = ThreatManager.alertSounds.play(ThreatManager.alertSoundsLoaded.get(alert.alertType.getSound()), 1, 1, 1, -1, 
				ThreatManager.getThreatSoundPitch(alert.strength));
	}
	
	void updateThreat(CobraMessageThreat t) {
		this.alert.frequency = t.frequency;
		this.alert.alertType = t.alertType;
		updateThreat(t.strength);
	}
	
	private void updateThreat(int newStrength) {
		if ( view == null || alert == null )
			return;
		recordLocation();
		// only update if strength changes
		if ( newStrength != alert.strength || !isShowing ) {
			
			this.alert.strength = newStrength;
			// stop any alert sound currently playing
			if ( soundStreamId != 0 ) {
				ThreatManager.alertSounds.stop(soundStreamId);
				soundStreamId = 0;
			}
			if ( !isShowing ) {
				ThreatManager.mainThreatLayout.addView(view);
				isShowing = true;
			}
			band.setText(alert.alertType.getName());
			freq.setText(Float.toString(alert.frequency) + " Ghz");
			ColorStateList threatColor = ColorStateList.valueOf(ThreatManager.getThreatColor(alert.strength)); 
			band.setTextColor(threatColor);
			freq.setTextColor(threatColor);
			strength.setProgress(this.alert.strength);
			//strength.getProgressDrawable().setColorFilter(getThreatColor(alert.strength), Mode.SRC_IN);
			playAlert();
		}
	}
	
	private void recordLocation() {
		if ( Preferences.isLogThreatLocation() && LocationManager.isReady() ) {
			if ( locations == null )
				locations = new LinkedHashSet<Location>();
			locations.add(LocationManager.getCurrentLoc());
		}
	}
	
}