package com.greatnowhere.iradar.threats;

import java.util.concurrent.atomic.AtomicBoolean;

import com.greatnowhere.iradar.config.Preferences;

import android.content.Context;
import android.media.AudioManager;

/**
 * Manages alerts volume
 * @author pzeltins
 *
 */
public class AlertAudioManager {
	
	/**
	 * If true, alert volume is already at "our" setting
	 */
	private static AtomicBoolean isOurAlertVolumeSet = new AtomicBoolean(false);
	private static AudioManager am;
	private static int originalVolume = 0;
	public static final int OUTPUT_STREAM = AudioManager.STREAM_MUSIC;
	
	
	public static void init(Context ctx) {
		am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
	}
	
	public static void setOurAlertVolume() {
		
		// only set volume if we already havent done so, and preferences indicate we should do it
		if ( Preferences.isSetAlertLevel() && !isOurAlertVolumeSet.get() ) {
			if ( am != null ) {
				originalVolume = am.getStreamVolume(OUTPUT_STREAM);
				// preferences alert level is always 0-7
				// must translate that to selected stream's level
				int translatedVolume = getTranslatedVolume(Preferences.getAlertLevel()); 
				am.setStreamVolume(OUTPUT_STREAM, translatedVolume, 0);
				isOurAlertVolumeSet.set(true);
			}
		}
		
	}
	
	public static void restoreOldAlertVolume() {

		if ( Preferences.isSetAlertLevel() ) {
			if ( am != null ) {
				am.setStreamVolume(OUTPUT_STREAM, originalVolume, 0);
				isOurAlertVolumeSet.set(false);
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
		return Math.round((((float) vol)/7f) * ((float) getMaxAudioVolume()));
	}
	
}
