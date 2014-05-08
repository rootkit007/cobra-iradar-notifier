package com.greatnowhere.iradar.receiver;

import com.greatnowhere.iradar.CobraIRadarActivity;
import com.greatnowhere.iradar.config.Preferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Class to detect when device hath booteth
 * @author pzeltins
 *
 */
public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if ( intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			if ( !Preferences.isInitialized() ) {
				Preferences.init(context);
			}
			
			if ( Preferences.isScanForDeviceAfterRestart() ) {
				Intent i = new Intent(context, CobraIRadarActivity.class);
				i.putExtra(CobraIRadarActivity.INTENT_BACKGROUND, true);
				context.startActivity(i);
			}
		}
	}

}
