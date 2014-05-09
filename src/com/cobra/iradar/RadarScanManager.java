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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.Configuration;

public class RadarScanManager {
	
	/**
	 * "Scanning" notification ID
	 */
	public static final int NOTIFICATION_SCAN = 1;

	private static Context ctx;
	private static AtomicBoolean isCarMode = new AtomicBoolean();
	private static UiModeManager uiManager;
	private static AlarmManager alarmManager;
	private static NotificationManager notifManager;
	private static PendingIntent reconnectionIntent;
	
	
	private static boolean runScan;
	private static int scanInterval;
	private static boolean runInCarModeOnly;
	private static Notification notify;
	
	private static EventBus eventBus = EventBus.getDefault();
	
	private static UIModeReceiver uiModeReceiver = new UIModeReceiver(); 
	
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
		RadarScanManager.notify = notify;
		uiManager = (UiModeManager) RadarScanManager.ctx.getSystemService(Context.UI_MODE_SERVICE);
		alarmManager = (AlarmManager) RadarScanManager.ctx.getSystemService(Context.ALARM_SERVICE);
		notifManager = (NotificationManager) RadarScanManager.ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		isCarMode.set( uiManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR );
		
		eventBus.register(new RadarEventsListener());
		IntentFilter intFilter = new IntentFilter();
		intFilter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
		intFilter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
		intFilter.addAction(UIModeReceiver.MOTO_X_CONTEXT_ACTION_DRIVE);
		intFilter.addAction(UIModeReceiver.MOTO_X_MODE_CHANGED_BROADCAST);
		ctx.registerReceiver(uiModeReceiver, intFilter);
		intFilter = new IntentFilter();
		intFilter.addAction(UIModeReceiver.MOTO_X_CONTEXT_CHANGE_BROADCAST);
		try {
			intFilter.addDataType(UIModeReceiver.MOTO_X_CONTEXT_BROADCAST_MIME_TYPE);
		} catch (MalformedMimeTypeException e) {
		}
		ctx.registerReceiver(uiModeReceiver, intFilter);
		
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
		if ( reconnectionIntent != null ) 
			alarmManager.cancel(reconnectionIntent);
		Intent reconnectIntent = new Intent(ctx, RadarMonitorService.class);
		reconnectIntent.putExtra(RadarMonitorService.INTENT_RECONNECT, true);
		reconnectionIntent = PendingIntent.getService(ctx, 0, reconnectIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000L, ((long) scanInterval) * 1000L, 
		  reconnectionIntent);
	}
	  
	public static void stop() {
		if ( reconnectionIntent != null ) {
			alarmManager.cancel(reconnectionIntent);
			reconnectionIntent = null;
			if ( notify != null ) {
				notifManager.cancel(NOTIFICATION_SCAN);
			}
			ctx.unregisterReceiver(uiModeReceiver);
		}
	}
	
	public static boolean isScanActive() {
		return ( reconnectionIntent != null );
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
	
	private static class UIModeReceiver extends BroadcastReceiver {
		
		private static String MOTO_X_CONTEXT_CHANGE_BROADCAST = "com.motorola.context.CONTEXT_CHANGE";
		private static String MOTO_X_CONTEXT_BROADCAST_MIME_TYPE = "context/com.motorola.context.publisher.InVehicle";
		private static String MOTO_X_CONTEXT_ACTION_DRIVE = "com.motorola.contextaware.intent.action.DRIVE_CURRENT_STATE";
		private static String MOTO_X_MODE_CHANGED_BROADCAST = "com.motorola.assist.intent.action.MODE_CHANGED";
		private static String MOTO_X_MODE_CHANGED_BROADCAST_STATE = "com.motorola.context.engine.intent.extra.MODE_CHANGED_STATUS";
		private static String MOTO_X_MODE_CHANGED_BROADCAST_STATE_PREV = "com.motorola.context.engine.intent.extra.MODE_CHANGED_PREV_STATUS";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if ( intent.getAction().equals(UiModeManager.ACTION_ENTER_CAR_MODE)  ) {
				isCarMode.set(true);
				eventBus.post(new RadarMessageNotification("Car mode activated"));
				scan();
			} 
			if ( intent.getAction().equals(UiModeManager.ACTION_EXIT_CAR_MODE)  ) {
				isCarMode.set(false);
				eventBus.post(new RadarMessageNotification("Car mode deactivated"));
				scan();
			}
			if ( intent.getAction().equals(UIModeReceiver.MOTO_X_CONTEXT_CHANGE_BROADCAST) &&
					intent.getType().equals(UIModeReceiver.MOTO_X_CONTEXT_BROADCAST_MIME_TYPE)) {
				eventBus.post(new RadarMessageNotification("Got Moto X context change event"));
			}
			if ( intent.getAction().equals(UIModeReceiver.MOTO_X_CONTEXT_ACTION_DRIVE)  ) {
				eventBus.post(new RadarMessageNotification("Got Moto X drive status event"));
			}
			if ( intent.getAction().equals(UIModeReceiver.MOTO_X_MODE_CHANGED_BROADCAST)  ) {
				int state = intent.getIntExtra(MOTO_X_MODE_CHANGED_BROADCAST_STATE, 0);
				int prevState = intent.getIntExtra(MOTO_X_MODE_CHANGED_BROADCAST_STATE_PREV, 0);
				eventBus.post(new RadarMessageNotification("Got Moto X status changed event: " + state + " prev " + prevState ));
			}
		}
	}
	
}
