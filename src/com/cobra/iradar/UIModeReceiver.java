package com.cobra.iradar;

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cobra.iradar.protocol.CobraRadarMessageNotification;

import de.greenrobot.event.EventBus;

public class UIModeReceiver extends BroadcastReceiver {
	
	private static EventBus eventBus;
	
	public static String MOTO_X_CONTEXT_CHANGE_BROADCAST = "com.motorola.context.CONTEXT_CHANGE";
	public static String MOTO_X_CONTEXT_BROADCAST_MIME_TYPE = "context/com.motorola.context.publisher.InVehicle";
	public static String MOTO_X_CONTEXT_ACTION_DRIVE = "com.motorola.contextaware.intent.action.DRIVE_CURRENT_STATE";
	public static String MOTO_X_MODE_CHANGED_BROADCAST = "com.motorola.assist.intent.action.MODE_CHANGED";
	public static String MOTO_X_MODE_CHANGED_BROADCAST_STATE = "com.motorola.context.engine.intent.extra.MODE_CHANGED_STATUS";
	public static String MOTO_X_MODE_CHANGED_BROADCAST_STATE_PREV = "com.motorola.context.engine.intent.extra.MODE_CHANGED_PREV_STATUS";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		eventBus = EventBus.getDefault();
		if ( intent.getAction().equals(Intent.ACTION_DOCK_EVENT) ) {
			if ( intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1) == Intent.EXTRA_DOCK_STATE_CAR ) {
				RadarScanManager.isCarMode.set(true);
				eventBus.post(new CobraRadarMessageNotification("Car mode activated"));
				RadarScanManager.scan();
			} else {
				RadarScanManager.isCarMode.set(false);
				eventBus.post(new CobraRadarMessageNotification("Car mode deactivated"));
				RadarScanManager.scan();
			}
		}
		if ( intent.getAction().equals(UiModeManager.ACTION_ENTER_CAR_MODE)  ) {
			RadarScanManager.isCarMode.set(true);
			eventBus.post(new CobraRadarMessageNotification("Car mode activated"));
			RadarScanManager.scan();
		} 
		if ( intent.getAction().equals(UiModeManager.ACTION_EXIT_CAR_MODE)  ) {
			RadarScanManager.isCarMode.set(false);
			eventBus.post(new CobraRadarMessageNotification("Car mode deactivated"));
			RadarScanManager.scan();
		}
		if ( intent.getAction().equals(UIModeReceiver.MOTO_X_CONTEXT_CHANGE_BROADCAST) &&
				intent.getType().equals(UIModeReceiver.MOTO_X_CONTEXT_BROADCAST_MIME_TYPE)) {
			eventBus.post(new CobraRadarMessageNotification("Got Moto X context change event"));
		}
		if ( intent.getAction().equals(UIModeReceiver.MOTO_X_CONTEXT_ACTION_DRIVE)  ) {
			eventBus.post(new CobraRadarMessageNotification("Got Moto X drive status event"));
		}
		if ( intent.getAction().equals(UIModeReceiver.MOTO_X_MODE_CHANGED_BROADCAST)  ) {
			int state = intent.getIntExtra(MOTO_X_MODE_CHANGED_BROADCAST_STATE, 0);
			int prevState = intent.getIntExtra(MOTO_X_MODE_CHANGED_BROADCAST_STATE_PREV, 0);
			eventBus.post(new CobraRadarMessageNotification("Got Moto X status changed event: " + state + " prev " + prevState ));
		}
	}
}