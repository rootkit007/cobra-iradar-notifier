package com.greatnowhere.radar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.greatnowhere.radar.location.PhoneActivityDetector;

/**
 * Receives UI mode change events
 * @author pzeltins
 *
 */
public class UIModeReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if ( intent.getAction().equals(Intent.ACTION_DOCK_EVENT) ) {
			if ( intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1) == Intent.EXTRA_DOCK_STATE_CAR ) {
				PhoneActivityDetector.setIsCarMode(true);
			} else {
				PhoneActivityDetector.setIsCarMode(false);
			}
		}
	}
}