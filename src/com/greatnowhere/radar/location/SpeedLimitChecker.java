package com.greatnowhere.radar.location;

import java.util.Timer;
import java.util.TimerTask;

import com.greatnowhere.radar.R;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.threats.AlertAudioManager;

import android.content.Context;
import android.media.SoundPool;
import android.util.Log;
import de.greenrobot.event.EventBus;

public class SpeedLimitChecker {
	
	private static final String TAG = SpeedLimitChecker.class.getCanonicalName();

	/**
	 * Overspeed in kmh at which overspeed alert sounds will repeat continuously (aka max alert level)
	 */
	public static final float MAX_OVERSPEED = 50f;
	
	/**
	 * Duration of our alert beep
	 */
	private static final int SOUND_DURATION_MS = 100;
	
	/**
	 * Repeat interval between alerts for min overspeed value (e.g. 0.1 kmh over limit)
	 */
	private static final int REPEAT_INTERVAL_MAX = 3000;
	
	private static EventBus eventBus;
	private static Context ctx;
	private static SpeedLimitChecker instance;
	
	private SoundPool soundPool;
	private int alertSoundId;
	private Timer timer;
	private AlertPlayerTask task = new AlertPlayerTask();
	
	public static void init(Context c) {
		ctx = c;
		eventBus = EventBus.getDefault();
		instance = new SpeedLimitChecker();
		eventBus.register(instance);
        instance.soundPool = new SoundPool(1, AlertAudioManager.OUTPUT_STREAM, 0);
        instance.alertSoundId = instance.soundPool.load(ctx, R.raw.threat, 1);
        instance.timer = new Timer(true);
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
	
	private void setAudibleWarning() {
		
		try {
			RadarLocationManager.LocationChanged locationEvent = eventBus.getStickyEvent(RadarLocationManager.LocationChanged.class);
			LocationInfoLookupManager.EventSpeedLimitChange speedLimitEvent = eventBus.getStickyEvent(LocationInfoLookupManager.EventSpeedLimitChange.class);
			if ( locationEvent == null ) {
				Log.w(TAG,"Location not known, cannot warn overspeed");
				return;
			}
			if ( speedLimitEvent == null ) {
				Log.w(TAG,"Speed limit not known, cannot warn overspeed");
				return;
			}
			float currentSpeed = locationEvent.getCurrentSpeedKph();
			int speedLimit = speedLimitEvent.getKPH();
					
			if (  ( currentSpeed - speedLimit) > Preferences.getWarnOverSpeedLimit() && isPreferenceSet() ) {
				AlertAudioManager.setOurAlertVolume();
				soundPool.play(alertSoundId, 1f, 1f, 1, 0, 1f);
				timer.schedule(task, getRepeatDelay(currentSpeed - speedLimit - Preferences.getWarnOverSpeedLimit()));
			} else {
				soundPool.stop(alertSoundId);
				AlertAudioManager.restoreOldAlertVolume();
				timer.cancel();
			}
		} catch (Exception ex) {
			Log.w(TAG,ex);
		}
		
	}
	
	/**
	 * Gets delay in ms between alert beep repeats
	 * @param overSpeed
	 * @return
	 */
	private int getRepeatDelay(float overSpeed) {
		return Math.round((SOUND_DURATION_MS + 
				( 1 - ( Math.min(overSpeed,MAX_OVERSPEED) / MAX_OVERSPEED ) ) * REPEAT_INTERVAL_MAX)); 
	}
	
	private static class AlertPlayerTask extends TimerTask {

		@Override
		public void run() {
			instance.setAudibleWarning();
		}
		
	}
	
}
