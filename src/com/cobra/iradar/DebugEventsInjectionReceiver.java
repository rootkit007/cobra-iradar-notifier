package com.cobra.iradar;

import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cobra.iradar.protocol.CobraRadarMessageAlert;
import com.cobra.iradar.protocol.CobraRadarMessageAlert.Alert;
import com.greatnowhere.radar.messaging.ConnectivityStatus;

import de.greenrobot.event.EventBus;

/**
 * Used to simulate various device events for debugging purposes
 * @author pzeltins
 *
 */
public class DebugEventsInjectionReceiver extends BroadcastReceiver {

	public static final String BROADCAST_INJECT_ALERT = "com.cobra.iradar.InjectRandomAlert";
	public static final String BROADCAST_INJECT_DEVCONNECT = "com.cobra.iradar.InjectDevConnected";
	public static final String BROADCAST_INJECT_DEVDISCONNECT = "com.cobra.iradar.InjectDevDisconnected";
	
	private static EventBus eventBus;
			
	@Override
	public void onReceive(Context context, Intent intent) {
		eventBus = EventBus.getDefault();
		if ( intent.getAction().equalsIgnoreCase(BROADCAST_INJECT_ALERT) ) {
			Random r = new Random();
			Alert a = Alert.Ka;
			switch (r.nextInt(2)) {
			case 0:
				a = Alert.K;
				break;
			case 1:
				a = Alert.Ka;
				break;
			case 2:
				a = Alert.X;
				break;
			}
			CobraRadarMessageAlert msg = new CobraRadarMessageAlert(a, r.nextInt(4), Math.round(r.nextFloat()*150f + 200f)/10f, r.nextInt(6000) );
			eventBus.post(msg);
		}
		if ( intent.getAction().equalsIgnoreCase(BROADCAST_INJECT_DEVCONNECT) ) {
			RadarConnectionThread.setConnectivityStatus(ConnectivityStatus.CONNECTED);
			eventBus.post(new CobraRadarEvents.EventDeviceConnected());
		}
		if ( intent.getAction().equalsIgnoreCase(BROADCAST_INJECT_DEVDISCONNECT) ) {
			RadarConnectionThread.setConnectivityStatus(ConnectivityStatus.DISCONNECTED);
			eventBus.post(new CobraRadarEvents.EventDeviceDisconnected());
		}
	}

}
