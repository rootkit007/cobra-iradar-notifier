package com.greatnowhere.iradar.receiver;

import com.greatnowhere.iradar.MainRadarActivity;
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
				Intent i = new Intent(context, MainRadarActivity.class);
				i.putExtra(MainRadarActivity.INTENT_BACKGROUND, true);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);
			}
		}
	}

}
