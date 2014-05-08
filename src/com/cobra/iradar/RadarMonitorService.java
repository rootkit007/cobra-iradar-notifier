package com.cobra.iradar;

import android.app.IntentService;
import android.content.Intent;

public class RadarMonitorService extends IntentService {

	public static final String INTENT_RECONNECT = "reconnect";

	public RadarMonitorService() {
		super(RadarMonitorService.class.getCanonicalName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if ( intent.getBooleanExtra(INTENT_RECONNECT, false) ) {
			RadarManager.tryReconnect();
		}
	}
	

}
