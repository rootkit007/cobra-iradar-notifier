package com.cobra.iradar;

import com.cobra.iradar.messaging.ConnectivityStatus;
import com.cobra.iradar.protocol.RadarMessageNotification;

import de.greenrobot.event.EventBus;

/**
 * Helper class for listening to connectivity events
 * @author pzeltins
 *
 */
public abstract class RadarConnectivityListener {
	
	private EventBus eventBus;
	
	public RadarConnectivityListener() {
		eventBus = EventBus.getDefault();
		eventBus.register(this);
	}
	
	public void stop() {
		if ( eventBus.isRegistered(this) )
			eventBus.unregister(this);
	}
	
	public void onEventAsync(RadarMessageNotification msg) {
		if ( msg.connectionStatus == ConnectivityStatus.CONNECTED.getCode() ) {
			onConnected();
		}
		if ( msg.connectionStatus == ConnectivityStatus.DISCONNECTED.getCode() ) {
			onDisconnected();
		}
	}
	
	public abstract void onConnected();
	public abstract void onDisconnected();

}
