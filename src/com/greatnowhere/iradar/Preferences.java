package com.greatnowhere.iradar;

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
	
	public static boolean isSetAlertLevel() {
		return prefs.getBoolean(res.getString(R.string.prefKeyAlertLevelSetFlag),true); 
	}
	
	public static int getAlertLevel() {
		int i = prefs.getInt(res.getString(R.string.prefKeyAlertLevel), 1);
		return i;
	}
	
	public static boolean isScanForDevice() {
		return prefs.getBoolean(res.getString(R.string.prefKeyScanForDevice), true);
	}
	
	public static int getDeviceScanInterval() {
		return Integer.parseInt(prefs.getString(res.getString(R.string.prefKeyScanInterval), "60"));
	}
	
	public static boolean isNotifyConnectivity() {
		return prefs.getBoolean(res.getString(R.string.prefKeySpeakEvents), true);
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
	
	private static class PreferenceChangeListener implements OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			
			// If periodic scanning has been changed, must notify the service
			if ( key.equalsIgnoreCase(res.getString(R.string.prefKeyScanForDevice)) ||
			     key.equalsIgnoreCase(res.getString(R.string.prefKeyScanForDevice)) ) {
				
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
