package com.greatnowhere.iradar.config;

import com.greatnowhere.iradar.R;

import de.greenrobot.event.EventBus;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class Preferences {

	private static SharedPreferences prefs;
	private static Resources res;
	private static EventBus eventBus = EventBus.getDefault();
	
	public static void init(Context ctx) {
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		res = ctx.getResources();
		prefs.registerOnSharedPreferenceChangeListener(new PreferenceChangeListener());
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
	
	public static boolean isScanForDevice() {
		return prefs.getBoolean(res.getString(R.string.prefKeyScanForDevice), true);
	}
	
	public static int getDeviceScanInterval() {
		return Integer.parseInt(prefs.getString(res.getString(R.string.prefKeyScanInterval), "60"));
	}
	
	public static boolean isScanForDeviceInCarModeOnly() {
		return prefs.getBoolean(res.getString(R.string.prefKeyScanOnlyInCarMode), true);
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
	
	private static class PreferenceChangeListener implements OnSharedPreferenceChangeListener {
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			
			// If periodic scanning has been changed, must notify the service
			if ( key.equalsIgnoreCase(res.getString(R.string.prefKeyScanForDevice)) ||
			     key.equalsIgnoreCase(res.getString(R.string.prefKeyScanInterval)) ||
			     key.equalsIgnoreCase(res.getString(R.string.prefKeyScanOnlyInCarMode))) {
				
				eventBus.post(new PreferenceScanChangedEvent());
				
			}
				
			
		}
	}
	
	/**
	 * Event class identifying that change occured to either "scan for device" or "scan interval" setting
	 * @author pzeltins
	 *
	 */
	public static class PreferenceScanChangedEvent {
		
	}
}
