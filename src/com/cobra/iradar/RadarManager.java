package com.cobra.iradar;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import de.greenrobot.event.EventBus;

/**
 * iRadar manager class. Initializes iRadar device, runs background connectivity task, and communicates with
 * UI app via messaging 
 * @author pzeltins
 *
 */
public class RadarManager {

	public static String TAG = RadarManager.class.getCanonicalName();
	  
	private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	private static BluetoothDevice mBTDevice = null;
	private static Context appContext;
	private static String lastError;
	private static boolean showNotification = false; 
	
	private static Notification ongoingNotification;
	
	@SuppressWarnings("unused")
	private static EventBus eventBus;
	  
	/**
	 * 
	 * @param ctx app context
	 * @param showNotification if true, service will show ongoing notification until stop() is called
	 * @param notificationTarget if showNotification=true must contain class of activity/task to be launched for the notification
	 * @param attemptReconnect if true, will automatically attempt reconnection if fails
	 * @param reconnectInterval interval in seconds
	 * @return True if radar device found and connection attempt started, false otherwise. Call {@link getLastError} to retrieve any error messages
	 */
	public static synchronized boolean initialize(Context ctx, boolean showNotification, Notification notify, Notification scanNotification,
			  boolean runScan, int scanInterval, boolean scanInCarModeOnly) {
		appContext = ctx;
		eventBus = EventBus.getDefault();
		RadarManager.showNotification = showNotification;
		RadarManager.ongoingNotification = notify;
		
		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
		    lastError = "Bluetooth is not available";
		    return false;
		}
		
		for ( BluetoothDevice dev : mBluetoothAdapter.getBondedDevices() ) {
			if ( Constants.BT_DEVICE_NAMES.contains(dev.getName()) ) {
				RadarScanManager.init(ctx, scanNotification, runScan, scanInterval, scanInCarModeOnly);
				connectRadarDevice(dev);
				return true;
			}
		}
		lastError = "iRadar device not paired!";
		return false;
	}
	  
	public static String getLastError() {
		return lastError;
	}
	  
	public static synchronized void stop() {
		appContext.stopService(new Intent(appContext, RadarConnectionService.class));
		RadarScanManager.stop();
	}
	  
	public static synchronized void startConnectionService() {
		if ( RadarManager.appContext == null )
			return;
		appContext.startService(new RadarConnectionServiceIntent(RadarManager.appContext, 
					  RadarManager.mBTDevice, RadarManager.ongoingNotification ));
	}
	  
	  
	private static synchronized void connectRadarDevice( BluetoothDevice dev ) {
		mBTDevice = dev;
		startConnectionService();
	}
	  
	public static boolean isShowNotification() {
		return showNotification;
	}
	
	public static void setShowNotification(boolean showNotification) {
		RadarManager.showNotification = showNotification;
	}
	
	public static Notification getOngoingNotification() {
		return ongoingNotification;
	}
	
	public static void setOngoingNotification(Notification ongoingNotification) {
		RadarManager.ongoingNotification = ongoingNotification;
	}

}
