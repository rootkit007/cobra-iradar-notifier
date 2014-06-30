package com.cobra.iradar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.greatnowhere.radar.messaging.ConnectivityStatus;

import de.greenrobot.event.EventBus;

public class RadarScanManager {
	
	private static final String TAG = RadarScanManager.class.getCanonicalName();
	
	/**
	 * "Scanning" notification ID
	 */
	public static final int NOTIFICATION_SCAN = 2;
	

	private static Context ctx;
	private static AlarmManager alarmManager;
	private static NotificationManager notifManager;
	private PendingIntent reconnectionIntent;
	
	
	private static boolean runScan;
	private static int scanInterval;
	private static Notification notify;
	
	private static EventBus eventBus;
	
	static RadarScanManager instance;
	
	/**
	 * 
	 * @param ctx Context
	 * @param notify If not null, will display ongoing notification while scan is active
	 * @param runScan if false, no scans will be active
	 * @param scanInterval scan interval in seconds
	 * @param runInCarModeOnly if true, will only run scan while in car mode
	 */
	public static void init(Context ctx, Notification notify) {
		RadarScanManager.ctx = ctx;
		instance = new RadarScanManager();
		eventBus = EventBus.getDefault();
		eventBus.register(instance);
		RadarScanManager.notify = notify;
		alarmManager = (AlarmManager) RadarScanManager.ctx.getSystemService(Context.ALARM_SERVICE);
		notifManager = (NotificationManager) RadarScanManager.ctx.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	public static void stop() {
		Log.i(TAG,"stop");
		runScan = false;
		stopAlarm();
	}
	
	public static void destroy() {
		Log.i(TAG,"destroy");
		stop();
		if ( instance != null && eventBus != null && eventBus.isRegistered(instance) ) {
			eventBus.unregister(instance);
			instance = null;
		}
	}
	
	public static boolean isInitialized() {
		return ( ctx != null );
	}
	
	public static void scan(boolean runScan, int scanInterval) {
		RadarScanManager.runScan = runScan;
		RadarScanManager.scanInterval = scanInterval;
		scan();
	}
	
	/**
	 * Manual one-time scan, forced regardless of other settings
	 */
	public static void scanForced() {
		RadarManager.startConnectionService();
	}
	
	public static void scan() {
		// determine if scan should be active
		// only if not connected
		if ( runScan && scanInterval > 0 && RadarManager.getConnectivityStatus() != ConnectivityStatus.CONNECTED ) {
			// yes, active. set up system alarm to wake monitor service
			stopAlarm();
			startConnectionMonitor();
		} else {
			// stop any current system alarms
			stop();
		}
	}
	
	/**
	 * Shows/hides notification as appropriate
	 */
	public static void showNotification() {
		if ( notifManager == null )
			return;
		if ( notify != null && runScan && RadarManager.getConnectivityStatus() != ConnectivityStatus.CONNECTED ) {
			notifManager.notify(NOTIFICATION_SCAN, notify);
		} else {
			notifManager.cancel(NOTIFICATION_SCAN);
		}
	}
	
	/**
	 * Starts periodic attempts to monitor/reconnect
	 * @param seconds
	 */
	private static void startConnectionMonitor() {
		Log.i(TAG,"start monitor");
		
		if ( instance != null && instance.reconnectionIntent != null ) 
			alarmManager.cancel(instance.reconnectionIntent);
		
		if ( instance != null && runScan && !RadarManager.isRadarConnected() ) {
			Log.d(TAG,"setting alarm for " + RadarMonitorService.class.getCanonicalName());
			Intent reconnectIntent = new Intent(ctx, RadarMonitorService.class);
			reconnectIntent.putExtra(RadarMonitorService.KEY_INTENT_RECONNECT, true);
			instance.reconnectionIntent = PendingIntent.getService(ctx, 0, reconnectIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000L, ((long) scanInterval) * 1000L, 
					instance.reconnectionIntent);
		}
	}
	  
	public static void stopAlarm() {
		Log.i(TAG,"stop alarm");

		if ( instance != null && instance.reconnectionIntent != null ) {
			Log.i(TAG,"cancel alarm " + instance.reconnectionIntent.toString());
			alarmManager.cancel(instance.reconnectionIntent);
			instance.reconnectionIntent.cancel();
			instance.reconnectionIntent = null;
		}

		showNotification();
	}
	
	public static boolean isScanActive() {
		return ( instance != null && instance.reconnectionIntent != null );
	}
	
	public static Notification getNotification() {
		return notify;
	}
	
	public static void setNotification(Notification n) {
		if ( n == null && notify != null )
			notifManager.cancel(NOTIFICATION_SCAN);
		notify = n;
		if ( n != null && isScanActive() ) 
			showNotification();
	}
	
	public void onEventMainThread(CobraRadarEvents.EventDeviceConnected event) {
		stopAlarm();
	}

	public void onEventMainThread(CobraRadarEvents.EventDeviceDisconnected event) {
		scan();
	}
	
}
