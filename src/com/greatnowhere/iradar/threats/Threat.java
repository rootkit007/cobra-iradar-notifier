package com.greatnowhere.iradar.threats;

import java.util.LinkedHashSet;
import java.util.Set;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff.Mode;
import android.location.Location;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cobra.iradar.messaging.CobraMessageThreat;
import com.greatnowhere.iradar.R;
import com.greatnowhere.iradar.config.Preferences;
import com.greatnowhere.iradar.location.LocationManager;
import com.greatnowhere.iradar.threats.ThreatManager.ThreatCredibility;

/**
 * Active threat class
 * @author pzeltins
 *
 */
public class Threat {
	
	public static final boolean SHOW_FALSE_THREATS = false;
	public static final boolean AUDIBLE_FALSE_THREATS = false;
	
	/**
	 * View displaying current threat
	 */
	private View view;
	protected CobraMessageThreat alert;
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
	
	/**
	 * True if this threat has been added to the view
	 */
	private boolean isShowing = false;
	
	public Threat() {
		
	}
	
	public Threat(View v, CobraMessageThreat a, ThreatManager.ThreatCredibility cred) {
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
		updateThreat(alert.strength);
	}
	
	void removeThreat() {
		endTimeMillis = System.currentTimeMillis();
		if ( isShowing )
			ThreatManager.post(new Runnable() {
				public void run() {
					ThreatManager.instance.mainThreatLayout.removeView(view);
				}
			});
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
		maxStrength = Math.max(maxStrength, newStrength);
		// only update if strength changes
		if ( newStrength != alert.strength || !isShowing ) {
			
			this.alert.strength = newStrength;
			// stop any alert sound currently playing
			if ( soundStreamId != 0 ) {
				ThreatManager.alertSounds.stop(soundStreamId);
				soundStreamId = 0;
			}
			if ( isShowVisibleThreat() ) {
				ThreatManager.post(new Runnable() {
					public void run() {
						if ( !isShowing ) {
							ThreatManager.instance.mainThreatLayout.addView(view);
							isShowing = true;
						}
						band.setText(alert.alertType.getName());
						freq.setText(Float.toString(alert.frequency) + " Ghz");
						int color = ThreatManager.getThreatColor(alert.strength);
						ColorStateList threatColor = ColorStateList.valueOf(color);
						band.setTextColor(threatColor);
						freq.setTextColor(threatColor);
						strength.setProgress(alert.strength);
						strength.getProgressDrawable().getCurrent().setColorFilter(color, Mode.MULTIPLY);
					}
				});
			}
			if ( isPlayAudibleThreat() )
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
				( SHOW_FALSE_THREATS );
	}

}