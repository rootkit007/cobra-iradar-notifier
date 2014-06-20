package com.greatnowhere.radar.location;

import android.content.Context;
import android.media.SoundPool;
import android.util.Log;

import com.greatnowhere.radar.R;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.threats.AlertAudioManager;

import de.greenrobot.event.EventBus;

public class SpeedLimitChecker {
	
	private static final String TAG = SpeedLimitChecker.class.getCanonicalName();

	/**
	 * Overspeed in kmh at which overspeed alert sounds will repeat continuously (aka max alert level)
	 */
	public static final float MAX_OVERSPEED = 50f;
	
	private static final float SOUND_MAX_PITCH = 1.5f;
	
	private static EventBus eventBus;
	private static Context ctx;
	private static SpeedLimitChecker instance;
	
	private SoundPool soundPool;
	private int alertSoundId;
	
	public static void init(Context c) {
		ctx = c;
		eventBus = EventBus.getDefault();
		instance = new SpeedLimitChecker();
		eventBus.register(instance);
        instance.soundPool = new SoundPool(1, AlertAudioManager.OUTPUT_STREAM, 0);
        instance.alertSoundId = instance.soundPool.load(ctx, R.raw.threat, 1);
	}
	
	public static void stop() {
		if ( instance != null ) {
			instance.soundPool.release();
			eventBus.unregister(instance);
			instance = null;
		}
	}
	
	/**
	 * Returns TRUE if speed limit should be checked as per preferences
	 * @return
	 */
	private boolean isPreferenceSet() {
		return Preferences.isWarnOverSpeed() && Preferences.isLookupSpeedLimit() && RadarLocationManager.isReady();
	}
	
	public void onEventAsync(Preferences.PreferenceOverSpeedSettingsChangedEvent event) {
		Log.i(TAG, "Got preference changed event");
		setAudibleWarning();
	}
	
	public void onEventAsync(LocationInfoLookupManager.EventSpeedLimitChange event) {
		Log.i(TAG, "Got speed limit change event");
		setAudibleWarning();
	}
	
	public void onEventAsync(RadarLocationManager.LocationChanged event) {
		Log.i(TAG, "Got location change event");
		setAudibleWarning();
	}
	
	private synchronized void setAudibleWarning() {
		
		try {
			RadarLocationManager.LocationChanged locationEvent = eventBus.getStickyEvent(RadarLocationManager.LocationChanged.class);
			LocationInfoLookupManager.EventSpeedLimitChange speedLimitEvent = eventBus.getStickyEvent(LocationInfoLookupManager.EventSpeedLimitChange.class);
			if ( locationEvent == null ) {
				Log.w(TAG,"Location not known, cannot warn overspeed");
				stopAudibleWarning();
				return;
			}
			if ( speedLimitEvent == null || speedLimitEvent.limit == null ) {
				Log.w(TAG,"Speed limit not known, cannot warn overspeed");
				stopAudibleWarning();
				return;
			}
			float currentSpeed = locationEvent.getCurrentSpeedKph();
			int speedLimit = speedLimitEvent.getKPH();
					
			if (  ( currentSpeed - speedLimit) > Preferences.getWarnOverSpeedLimit() && isPreferenceSet() ) {
				AlertAudioManager.setOurAlertVolume();
				soundPool.play(alertSoundId, 1f, 1f, 1, 0, getAlertPitch( currentSpeed - speedLimit - Preferences.getWarnOverSpeedLimit() ));
			} else {
				stopAudibleWarning();
			}
		} catch (Exception ex) {
			Log.w(TAG,ex);
		}
		
	}
	
	private synchronized void stopAudibleWarning() {
		soundPool.stop(alertSoundId);
		AlertAudioManager.restoreOldAlertVolume();
	}
	
	/**
	 * Gets sound pitch
	 * @param overSpeed
	 * @return
	 */
	private float getAlertPitch(float overSpeed) {
		return ( Math.min(overSpeed,MAX_OVERSPEED) / MAX_OVERSPEED ) * SOUND_MAX_PITCH;
	}
	
}
