package com.greatnowhere.radar.threats;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.greatnowhere.radar.config.Preferences;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * Manages alerts volume
 * @author pzeltins
 *
 */
public class AlertAudioManager {
	
	private static final String TAG = AlertAudioManager.class.getCanonicalName();
	
	/**
	 * If true, alert volume is already at "our" setting
	 */
	private static AtomicBoolean isOurAlertVolumeSet = new AtomicBoolean(false);
	private static AudioManager am;
	private static int originalVolume = 0;
	private static AtomicInteger volumeSetCounter = new AtomicInteger(0);
	public static final int OUTPUT_STREAM = AudioManager.STREAM_MUSIC;
	public static final int AUTOMUTE_VOLUME_OFFSET = -2;
	
	public static void init(Context ctx) {
		am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
	}
	
	public synchronized static void setOurAlertVolume() {
		Log.i(TAG,"setOurAlertVolume()");
		// only set volume if we already havent done so, and preferences indicate we should do it
		if ( Preferences.isSetAlertLevel() ) {
			if ( am != null ) {
				if ( !isOurAlertVolumeSet.get() ) 
					originalVolume = am.getStreamVolume(OUTPUT_STREAM);
				volumeSetCounter.addAndGet(1);
				// preferences alert level is always 0-7
				// must translate that to selected stream's level
				int translatedVolume = getTranslatedVolume(getOurAlertLevel());
				// only set volume if it means increase, never decrease
				if ( originalVolume < translatedVolume ) {
					am.setStreamVolume(OUTPUT_STREAM, translatedVolume, 0);
					Log.i(TAG,"Changed volume from " + originalVolume + " to " + translatedVolume);
				}
				isOurAlertVolumeSet.set(true);
			}
		}
	}

	/**
	 * Adjusts current volume for TTS
	 */
	public synchronized static void setTTSVolume() {
		setOurAlertVolume();
		if ( Preferences.isSetAlertLevel() && Preferences.getNotificationVolumeOffset() != 0 ) {
			int ttsVolume = getTranslatedVolume(getOurAlertLevel() + Preferences.getNotificationVolumeOffset());
			if ( ttsVolume >  am.getStreamVolume(OUTPUT_STREAM) ) {
				am.setStreamVolume(OUTPUT_STREAM, ttsVolume, 0);
				Log.i(TAG,"Changed TTS volume to " + ttsVolume);
			}
		}
		
	}
	
	/**
	 * Returns alert level for current audio output route
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private static int getOurAlertLevel() {
		if ( am.isBluetoothA2dpOn() || am.isBluetoothScoOn() ) {
			// BT
			return Preferences.getAlertLevelBT();
		}
		if ( am.isWiredHeadsetOn()) {
			return Preferences.getAlertLevelHeadSet();
		}
		return Preferences.getAlertLevelSpeaker();
	}
	
	public synchronized static void restoreOldAlertVolume() {
		Log.i(TAG,"restoreOldAlertVolume()");
		if ( Preferences.isSetAlertLevel() && isOurAlertVolumeSet.get() && volumeSetCounter.addAndGet(-1) == 0 ) {
			if ( am != null ) {
				am.setStreamVolume(OUTPUT_STREAM, originalVolume, 0);
				isOurAlertVolumeSet.set(false);
				Log.i(TAG,"Restored volume to " + originalVolume);
			}
		}
	}
	
	public static int getMaxAudioVolume() {
		return am.getStreamMaxVolume(OUTPUT_STREAM);
	}
	
	/**
	 * Translates 0-7 based volume into our output stream's volume index
	 * @param vol
	 * @return
	 */
	private static int getTranslatedVolume(int vol) {
		// ensure volume is in 0-7 range
		vol = Math.max(vol, 0);
		vol = Math.min(vol, 7);
		return Math.round((((float) vol)/7f) * ((float) getMaxAudioVolume()));
	}
	
}
