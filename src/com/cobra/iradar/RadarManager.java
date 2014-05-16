package com.cobra.iradar;

import com.greatnowhere.radar.messaging.RadarMessage;
import com.greatnowhere.radar.messaging.ConnectivityStatus;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
	/**
	 * Service to which send device activity intents
	 */
	private static Class<? extends Service> serviceClass;
	
	public static final String INTENT_ACTIVITY_EXTRA_KEY_MSG = "cobraMessageKey";
	
	private static Notification ongoingNotification;
	
	@SuppressWarnings("unused")
	private static EventBus eventBus;
	
	private static ListenerToIntent listenerToIntent;
	  
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
			  boolean runScan, int scanInterval, boolean scanInCarModeOnly, Class<? extends Service> serviceClass) {
		appContext = ctx;
		eventBus = EventBus.getDefault();
		RadarManager.showNotification = showNotification;
		RadarManager.ongoingNotification = notify;
		RadarManager.serviceClass = serviceClass;
		if ( listenerToIntent != null )
			listenerToIntent.stop();
		
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
		if ( listenerToIntent != null ) {
			listenerToIntent.stop();
		}
		RadarScanManager.stop();
		appContext.stopService(new Intent(appContext, RadarConnectionService.class));
	}
	  
	public static synchronized void startConnectionService() {
		if ( RadarManager.appContext == null )
			return;
		if ( listenerToIntent != null )
			listenerToIntent.stop();
		listenerToIntent = new ListenerToIntent();
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
	
	public static double getBatteryVoltage() {
		return listenerToIntent.getBatteryVoltage();
	}
	
	public static ConnectivityStatus getConnectivityStatus() {
		return RadarConnectionThread.getConnectivityStatus();
	}
	
	/**
	 * Notifies data collector service via intents
	 * This ensures collector service is up and running for all events
	 * @author pzeltins
	 *
	 */
	private static class ListenerToIntent extends RadarMessageHandler {
		private static final String TAG = ListenerToIntent.class.getCanonicalName();
		
		public ListenerToIntent() {
			super();
			eventBus = EventBus.getDefault();
			eventBus.register(this);
		}
		
		public void onRadarMessage(RadarMessage msg) {
			Log.i(TAG,"Got Cobra message " + msg.toString());
			Intent i = new Intent(appContext, serviceClass);
			i.putExtra(INTENT_ACTIVITY_EXTRA_KEY_MSG, msg);
			appContext.startService(i);
		}
	}
	


}
