package com.cobra.iradar;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class RadarMonitorService extends IntentService {

	private static final String TAG = RadarMonitorService.class.getCanonicalName();
	
	public static final String KEY_INTENT_RECONNECT = "reconnect";

	public RadarMonitorService() {
		super(RadarMonitorService.class.getCanonicalName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "Service started");
		if ( intent != null && intent.getBooleanExtra(KEY_INTENT_RECONNECT, false) ) {
			Log.i(TAG, "Connection attempt initiated");
			RadarScanManager.showNotification();
			RadarManager.startConnectionService();
		}
	}
	

}
