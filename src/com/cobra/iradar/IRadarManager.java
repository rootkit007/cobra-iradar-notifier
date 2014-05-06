package com.cobra.iradar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
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
public class IRadarManager {

	public static String TAG = IRadarManager.class.getCanonicalName();
	  
	private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	private static BluetoothDevice mBTDevice = null;
	private static Context appContext;
	private static PendingIntent reconnectionIntent;
	private static String lastError;
	private static boolean showNotification = false; 
	
	private static Notification ongoingNotification;
	private static boolean attemptReconnect = true;
	private static int reconnectInterval = 60;
	
	private static EventBus eventBus = EventBus.getDefault();
	  
	/**
	 * 
	 * @param ctx app context
	 * @param showNotification if true, service will show ongoing notification until stop() is called
	 * @param notificationTarget if showNotification=true must contain class of activity/task to be launched for the notification
	 * @param attemptReconnect if true, will automatically attempt reconnection if fails
	 * @param reconnectInterval interval in seconds
	 * @return True if radar device found and connection attempt started, false otherwise. Call {@link getLastError} to retrieve any error messages
	 */
	public static synchronized boolean initialize(Context ctx, boolean showNotification, Notification notify, 
			  boolean attemptReconnect, int reconnectInterval) {
		  	appContext = ctx;
        	IRadarManager.showNotification = showNotification;
        	IRadarManager.setAttemptReconnect(attemptReconnect);
        	IRadarManager.ongoingNotification = notify;
		  	
	        // If the adapter is null, then Bluetooth is not supported
	        if (mBluetoothAdapter == null) {
	            lastError = "Bluetooth is not available";
	            return false;
	        }
	        
	        for ( BluetoothDevice dev : mBluetoothAdapter.getBondedDevices() ) {
	        	if ( Constants.BT_DEVICE_NAMES.contains(dev.getName()) ) {
	        		connectRadarDevice(dev);
	        		if ( attemptReconnect )
	        			startConnectionMonitor(IRadarManager.reconnectInterval);
	        		
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
	  }
	  
	  public static synchronized void tryReconnect() {
		  eventBus.post(new RadarConnectionService.EventReconnectionAttemptCommand());
	  }
	  
	  /**
	   * Starts periodic attempts to monitor/reconnect
	   * @param seconds
	   */
	  public static void startConnectionMonitor(int seconds) {
		  setAttemptReconnect(true);
		  reconnectInterval = seconds;
		  AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		  Intent reconnectIntent = new Intent(appContext, RadarMonitorService.class);
		  reconnectIntent.putExtra(RadarMonitorService.INTENT_RECONNECT, true);
		  reconnectionIntent = PendingIntent.getService(appContext, 0, reconnectIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		  am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000L, ((long) seconds) * 1000L, 
				  reconnectionIntent);
	  }
	  
	  public static void stopConnectionMonitor() {
		  if ( reconnectionIntent != null ) {
			  AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
			  am.cancel(reconnectionIntent);
			  reconnectionIntent = null;
		  }
		  setAttemptReconnect(false);
	  }
	  
	private static synchronized void connectRadarDevice( BluetoothDevice dev ) {
		mBTDevice = dev;
		appContext.startService(new RadarConnectionServiceIntent(IRadarManager.appContext, 
			  IRadarManager.mBTDevice, IRadarManager.ongoingNotification ));
	}
	  
	public static boolean isShowNotification() {
		return showNotification;
	}
	
	public static void setShowNotification(boolean showNotification) {
		IRadarManager.showNotification = showNotification;
	}
	
	public static Notification getOngoingNotification() {
		return ongoingNotification;
	}
	
	public static void setOngoingNotification(Notification ongoingNotification) {
		IRadarManager.ongoingNotification = ongoingNotification;
	}

	public static boolean isAttemptReconnect() {
		return attemptReconnect;
	}

	public static void setAttemptReconnect(boolean attemptReconnect) {
		IRadarManager.attemptReconnect = attemptReconnect;
	}
	
}
