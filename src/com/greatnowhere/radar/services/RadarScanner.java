package com.greatnowhere.radar.services;

import android.util.Log;

import com.cobra.iradar.RadarScanManager;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.config.Preferences.PreferenceDeviceScanSettingsChangedEvent;
import com.greatnowhere.radar.config.Preferences.PreferenceOngoingNotificationsChangedEvent;
import com.greatnowhere.radar.location.PhoneActivityDetector;
import com.greatnowhere.radar.location.PhoneActivityDetector.ActivityStatus;
import com.greatnowhere.radar.location.PhoneActivityDetector.EventActivityChanged;
import com.greatnowhere.radar.location.PhoneActivityDetector.EventCarModeChange;
import com.greatnowhere.radar.util.AbstractEventBusListener;

/**
 * Starts/stops radar scanner service depending on configuration, events etc
 * @author pzeltins
 *
 */
public class RadarScanner extends AbstractEventBusListener {

	private static final String TAG = RadarScanner.class.getCanonicalName();
	
	private static RadarScanner instance;
	
	private RadarScanner() {
		super();
	}
	
	public static void init() {
		instance = new RadarScanner();
		scan();
	}
	
	public static RadarScanner getInstance() {
		return instance;
	}
	
	public static boolean isScanAllowed() {
		boolean runScan = 
				( Preferences.isScanForDevice() ) && // if scan is enabled at all
				( Preferences.getDeviceScanInterval() > 0 ) && // and scan interval is defined
				( Preferences.isScanForDeviceInCarModeOnly() ? PhoneActivityDetector.getIsCarMode() : true ) && // car mode
				// driving mode
				( Preferences.isScanForDeviceInDrivingModeOnly() ? 
						PhoneActivityDetector.getActivityStatus() == ActivityStatus.DRIVING : true);
		return runScan;
	}
	
	// decide whether to run scanner
	public static synchronized void scan() {
		boolean scanAllowed = isScanAllowed(); 
		if ( scanAllowed ) {
			Log.d(TAG,"scan allowed, starting");
			RadarScanManager.scan(scanAllowed, Preferences.getDeviceScanInterval());
		} else {
			Log.d(TAG,"scan prohibited, stopping");
			RadarScanManager.stop();
		}
	}
	
	public static void stop() {
    	Log.d(TAG, "stop");
		if ( getInstance() != null ) {
			getInstance().unRegister();
			instance = null;
		}
		RadarScanManager.stop();
	}
	
	// Preferences changed listener
	public void onEventBackgroundThread(PreferenceDeviceScanSettingsChangedEvent event) {
		scan();
	}
	
	// Phone activity changed listener
	public void onEventBackgroundThread(EventActivityChanged event) {
		scan();
	}

	// Car mode changed listener
	public void onEventBackgroundThread(EventCarModeChange event) {
		scan();
	}

	// Notification settings change listener
	public void onEventBackgroundThread(PreferenceOngoingNotificationsChangedEvent event) {
		// TODO: notif
		scan();
	}
}
