package com.greatnowhere.radar.config;

import com.greatnowhere.radar.R;

import de.greenrobot.event.EventBus;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class Preferences {

	public static final int PREF_UNITS_METRIC = 1;
	public static final int PREF_UNITS_IMPERIAL = 2;
	
	private static final String DEFAULT_LOG_FILE_NAME = "com.greatnowhere.radar.log";
	
	private static SharedPreferences prefs;
	private static Resources res;
	private static EventBus eventBus = EventBus.getDefault();
	
	public static void init(Context ctx) {
		if ( prefs == null ) {
			prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			res = ctx.getResources();
			prefs.registerOnSharedPreferenceChangeListener(new PreferenceChangeListener());
	        // set defaults for preferences
	        PreferenceManager.setDefaultValues(ctx, R.xml.settings, false);
		}
	}
	
	public static boolean isInitialized() {
		return ( prefs != null );
	}
	
	public static boolean isSetAlertLevel() {
		return prefs.getBoolean(res.getString(R.string.prefKeyAlertLevelSetFlag),true); 
	}
	
	public static int getAlertLevelSpeaker() {
		int i = prefs.getInt(res.getString(R.string.prefKeyAlertLevel), 1);
		return i;
	}
	
	public static int getAlertLevelBT() {
		int i = prefs.getInt(res.getString(R.string.prefKeyAlertLevelBT), 1);
		return i;
	}
	
	public static int getAlertLevelHeadSet() {
		int i = prefs.getInt(res.getString(R.string.prefKeyAlertLevelHeadSet), 1);
		return i;
	}
	
	public static int getAlertAutoMuteDelay() {
		String v = prefs.getString(res.getString(R.string.prefKeyAutoMuteDelay), Integer.toString(5));
		return Integer.parseInt(v);
	}
	
	public static boolean isAutoMuteImmediatelyDuringCalls() {
		return prefs.getBoolean(res.getString(R.string.prefKeyAutoMuteImmediatelyDuringCalls), true);
	}
	
	public static boolean isScanForDevice() {
		return prefs.getBoolean(res.getString(R.string.prefKeyScanForDevice), true);
	}
	
	public static int getDeviceScanInterval() {
		return Integer.parseInt(prefs.getString(res.getString(R.string.prefKeyScanInterval), "60"));
	}
	
	public static boolean isScanForDeviceInCarModeOnly() {
		return prefs.getBoolean(res.getString(R.string.prefKeyScanOnlyInCarMode), false);
	}
	
	public static boolean isScanForDeviceInDrivingModeOnly() {
		return prefs.getBoolean(res.getString(R.string.prefKeyScanOnlyInDrivingMode), false);
	}
	
	public static boolean isScanForDeviceAfterRestart() {
		return prefs.getBoolean(res.getString(R.string.prefKeyStartScanOnBoot), false);
	}
	
	/**
	 * Ongoing notification while scanning?
	 * @return
	 */
	public static boolean isNotifyOngoingScan() {
		return prefs.getBoolean(res.getString(R.string.prefKeyOngoingNotificationWhileScanning), true);
	}
	
	/**
	 * Ongoing notification while connected?
	 * @return
	 */
	public static boolean isNotifyOngoingConnected() {
		return prefs.getBoolean(res.getString(R.string.prefKeyOngoingNotificationWhileConnected), true);
	}
	
	public static boolean isNotifyConnectivity() {
		return prefs.getBoolean(res.getString(R.string.prefKeySpeakEvents), true);
	}

	/**
	 * If true, dont speak connectivity events during calls 
	 * @return
	 */
	public static boolean isNotifyConnectivityNotDuringCalls() {
		return prefs.getBoolean(res.getString(R.string.prefKeySpeakNotDuringCalls), true);
	}
	
	public static String getNotifyOnConnectText() {
		return prefs.getString(res.getString(R.string.prefKeyTextDeviceConnected), "");
	}
	
	public static String getNotifyWhileConnectedText() {
		return prefs.getString(res.getString(R.string.prefKeyTextDeviceWorking), "");
	}
	
	public static String getNotifyOnDisconnectText() {
		return prefs.getString(res.getString(R.string.prefKeyTextDeviceDisconnect), "");
	}
	
	public static int getNotifyWhileConnectedInterval() {
		return Integer.parseInt(prefs.getString(res.getString(R.string.prefKeyTextDeviceWorkingInterval), "300"));
	}
	
	/**
	 * Offset for TTS volume
	 * @return
	 */
	public static int getNotificationVolumeOffset() {
		return Integer.parseInt(prefs.getString(res.getString(R.string.prefKeyTTSVolumeOffset), "0"));
	}
	
	public static boolean isKeepScreenOnForeground() {
		return prefs.getBoolean(res.getString(R.string.prefKeyKeepScreenOnInForeground), true);
	}
	
	public static boolean isTurnScreenOnForAlerts() {
		return prefs.getBoolean(res.getString(R.string.prefKeyTurnScreenOnForAlerts), true);
	}
	
	public static boolean isLogThreats() {
		return prefs.getBoolean(res.getString(R.string.prefKeyLogThreats), true);
	}
	
	public static boolean isLogThreatLocation() {
		return prefs.getBoolean(res.getString(R.string.prefKeyLogLocation), true);
	}
	
	public static boolean isLogThreatLimitNumeric() {
		return prefs.getBoolean(res.getString(R.string.prefKeyLogThreatsLimitNum), true);
	}
	
	public static int getLogThreatLimitNumeric() {
		return prefs.getInt(res.getString(R.string.prefKeyLogThreatsLimitNumVal), 300);
	}
	
	public static boolean isFakeAlertDetection() {
		return prefs.getBoolean(res.getString(R.string.prefKeyFakeAlertDetection), true);
	}
	
	public static float getFakeAlertDetectionRadius() {
		String v = prefs.getString(res.getString(R.string.prefKeyFakeAlertDetectionRadius), Float.toString(0.1f));
		return Float.parseFloat(v);
	}
	
	public static int getFakeAlertOccurenceThreshold() {
		String v = prefs.getString(res.getString(R.string.prefKeyFakeAlertDetectionOccurenceThreshold), Integer.toString(5));
		return Integer.parseInt(v);
	}
	
	public static int getThreatShowMinSpeed() {
		String v = prefs.getString(res.getString(R.string.prefKeyThreatShowMinSpeed), Integer.toString(0));
		return Integer.parseInt(v);
	}
	
	public static boolean isShowHiddenThreats() {
		return prefs.getBoolean(res.getString(R.string.prefKeyShowFakeHiddenThreats), true);
	}
	
	public static String getLogFileName() {
		return prefs.getString(res.getString(R.string.prefKeyLogFileName), DEFAULT_LOG_FILE_NAME);
	}
	
	/**
	 * How many lines to display in log
	 * @return
	 */
	public static int getScreenLogScrollBackLimit() {
		return prefs.getInt(res.getString(R.string.prefKeyScreenLogScrollBackLimit), 300);
	}
	
	/**
	 * Screen Refresh Frequency (Hz) in main activity 
	 * @return
	 */
	public static int getScreenRefreshFrequency() {
		return prefs.getInt(res.getString(R.string.prefKeyScreenRefreshFrequency), 2);
	}
	
	public static int getUnits() {
		String v = prefs.getString(res.getString(R.string.prefKeyUnits), Integer.toString(PREF_UNITS_METRIC));
		return Integer.parseInt(v); 
	}
	
	private static class PreferenceChangeListener implements OnSharedPreferenceChangeListener {
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			
			// If periodic scanning has been changed, must notify the service
			if ( key.equalsIgnoreCase(res.getString(R.string.prefKeyScanForDevice)) ||
			     key.equalsIgnoreCase(res.getString(R.string.prefKeyScanInterval)) ||
			     key.equalsIgnoreCase(res.getString(R.string.prefKeyScanOnlyInDrivingMode)) ||
			     key.equalsIgnoreCase(res.getString(R.string.prefKeyScanOnlyInCarMode))) {
				
				eventBus.post(new PreferenceDeviceScanSettingsChangedEvent());
				
			}
				
			// If periodic scanning has been changed, must notify the service
			if ( key.equalsIgnoreCase(res.getString(R.string.prefKeyOngoingNotificationWhileConnected)) ||
				 key.equalsIgnoreCase(res.getString(R.string.prefKeyOngoingNotificationWhileScanning)) ) {

				eventBus.post(new PreferenceOngoingNotificationsChangedEvent());

			}
			
		}
	}
	
	/**
	 * Event class identifying that change occured to either "scan for device" or "scan interval" setting
	 * @author pzeltins
	 *
	 */
	public static class PreferenceDeviceScanSettingsChangedEvent {
		
	}
	
	public static class PreferenceOngoingNotificationsChangedEvent {
		
	}
}
