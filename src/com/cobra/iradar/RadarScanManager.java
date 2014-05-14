package com.cobra.iradar;

import java.util.concurrent.atomic.AtomicBoolean;

import com.cobra.iradar.messaging.ConnectivityStatus;
import com.cobra.iradar.protocol.RadarMessageNotification;

import de.greenrobot.event.EventBus;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

public class RadarScanManager {
	
	/**
	 * "Scanning" notification ID
	 */
	public static final int NOTIFICATION_SCAN = 1;

	private static Context ctx;
	static AtomicBoolean isCarMode = new AtomicBoolean();
	private static UiModeManager uiManager;
	private static AlarmManager alarmManager;
	private static NotificationManager notifManager;
	private PendingIntent reconnectionIntent;
	
	
	private static boolean runScan;
	private static int scanInterval;
	private static boolean runInCarModeOnly;
	private static Notification notify;
	
	private EventBus eventBus;
	private RadarEventsListener radarEventsListener;
	static RadarScanManager instance;
	
	/**
	 * 
	 * @param ctx Context
	 * @param notify If not null, will display ongoing notification while scan is active
	 * @param runScan if false, no scans will be active
	 * @param scanInterval scan interval in seconds
	 * @param runInCarModeOnly if true, will only run scan while in car mode
	 */
	public static void init(Context ctx, Notification notify, boolean runScan, int scanInterval, boolean runInCarModeOnly) {
		RadarScanManager.ctx = ctx;
		instance = new RadarScanManager();
		instance.eventBus = EventBus.getDefault();
		instance.radarEventsListener = new RadarEventsListener();
		RadarScanManager.notify = notify;
		uiManager = (UiModeManager) RadarScanManager.ctx.getSystemService(Context.UI_MODE_SERVICE);
		alarmManager = (AlarmManager) RadarScanManager.ctx.getSystemService(Context.ALARM_SERVICE);
		notifManager = (NotificationManager) RadarScanManager.ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		isCarMode.set( uiManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR );
		
		instance.eventBus.register(instance.radarEventsListener);
		scan(runScan,scanInterval,runInCarModeOnly);
	}
	
	public static boolean isInitialized() {
		return ( ctx != null );
	}
	
	public static void scan(boolean runScan, int scanInterval, boolean runInCarModeOnly) {
		RadarScanManager.runScan = runScan;
		RadarScanManager.scanInterval = scanInterval;
		RadarScanManager.runInCarModeOnly = runInCarModeOnly;
		scan();
	}
	
	public static void scan() {
		// determine if scan should be active
		if ( runScan && scanInterval > 0 &&
				  ( !runInCarModeOnly || ( runInCarModeOnly && isCarMode.get() ) ) ) {
			
			// yes, active. set up system alarm to wake monitor service
			stop();
			startConnectionMonitor();
		} else {
			// stop any current system alarms
			stop();
		}
	}
	
	/**
	 * Starts periodic attempts to monitor/reconnect
	 * @param seconds
	 */
	private static void startConnectionMonitor() {
		if ( notify != null )
			notifManager.notify(NOTIFICATION_SCAN, notify);
		if ( instance.reconnectionIntent != null ) 
			alarmManager.cancel(instance.reconnectionIntent);
		if ( !instance.eventBus.isRegistered(instance.radarEventsListener))
			instance.eventBus.register(instance.radarEventsListener);
		
		Intent reconnectIntent = new Intent(ctx, RadarMonitorService.class);
		reconnectIntent.putExtra(RadarMonitorService.INTENT_RECONNECT, true);
		instance.reconnectionIntent = PendingIntent.getService(ctx, 0, reconnectIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000L, ((long) scanInterval) * 1000L, 
				instance.reconnectionIntent);
	}
	  
	public static void stop() {
		if ( instance.reconnectionIntent == null ) {
			Intent reconnectIntent = new Intent(ctx, RadarMonitorService.class);
			reconnectIntent.putExtra(RadarMonitorService.INTENT_RECONNECT, true);
			instance.reconnectionIntent = PendingIntent.getService(ctx, 0, reconnectIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		}
		alarmManager.cancel(instance.reconnectionIntent);
		instance.reconnectionIntent = null;

		if ( notify != null ) {
			notifManager.cancel(NOTIFICATION_SCAN);
		}
		if ( instance.eventBus.isRegistered(instance.radarEventsListener)) 
			instance.eventBus.unregister(instance.radarEventsListener);
	}
	
	public static boolean isScanActive() {
		return ( instance.reconnectionIntent != null );
	}
	
	// Run scan only when not connected ya know
	private static class RadarEventsListener {
		@SuppressWarnings("unused")
		public void onEventAsync(RadarMessageNotification msg) {
			if ( msg.type == RadarMessageNotification.TYPE_CONN ) {
				switch (ConnectivityStatus.fromCode(msg.connectionStatus)) {
				case CONNECTED:
					stop();
					break;
				case DISCONNECTED:
					scan();
					break;
				default:
				}
			}
		}
	}
	
}
