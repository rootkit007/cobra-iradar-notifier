package com.cobra.iradar;

import com.cobra.iradar.protocol.CobraRadarMessageNotification;
import com.greatnowhere.radar.messaging.ConnectivityStatus;
import com.greatnowhere.radar.util.AbstractEventBusListener;

/**
 * Helper class for listening to connectivity events
 * @author pzeltins
 *
 */
public abstract class RadarConnectivityListener extends AbstractEventBusListener {
	
	public void onEventAsync(CobraRadarMessageNotification msg) {
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
