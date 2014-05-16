package com.cobra.iradar;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;

import com.cobra.iradar.protocol.CobraRadarMessageAlert;
import com.cobra.iradar.protocol.CobraRadarMessageNotification;
import com.cobra.iradar.protocol.CobraRadarMessageStopAlert;
import com.greatnowhere.radar.messaging.RadarMessage;
import com.greatnowhere.radar.messaging.RadarMessageAllClear;
import com.greatnowhere.radar.messaging.RadarMessageConnectivityNotification;
import com.greatnowhere.radar.messaging.RadarMessageNotification;
import com.greatnowhere.radar.messaging.RadarMessageThreat;
import com.greatnowhere.radar.messaging.ConnectivityStatus;

import de.greenrobot.event.EventBus;

/**
 * 
 * This handler is used for one-way communication from radar device to user app 
 * Handler class to receive messages from iRadar device
 * User app must extend this class and implement onRadarMessage() methods
 * 
 * @author pzeltins
 *
 */
public abstract class RadarMessageHandler {

	private static final String TAG = RadarMessageHandler.class.getCanonicalName();
	
	private ConnectivityStatus connStatus = ConnectivityStatus.UNKNOWN;
	private Double batteryVoltage = 0D;
	private boolean isThreatActive = false;
	private Timer alertTimer;
	/**
	 * True if threat is forcibly held active, regardless of "All Clear" messages
	 * Used mostly for testing purposes
	 */
	protected AtomicBoolean isThreatForcedActive = new AtomicBoolean(false);
	
	protected static EventBus eventBus;
	
	public void stop() {
		if ( eventBus != null && eventBus.isRegistered(this) )
			eventBus.unregister(this);
	}
	
	/**
	 * Receives raw messages from {@link RadarConnectionService} and calls onRadarMessage as needed 
	 */
    public final void onEventAsync(CobraRadarMessageNotification msg) {
    	
    	Log.i(TAG,"Got " + msg.toString());
    	
    	switch (msg.type) {
    	case CobraRadarMessageNotification.TYPE_CONN:
        	RadarMessageConnectivityNotification msgConn = new RadarMessageConnectivityNotification();
        	msgConn.message = msg.message;
        	msgConn.status = ConnectivityStatus.fromCode(msg.connectionStatus);
        	connStatus = msgConn.status;
        	onRadarMessage(msgConn);
        	break;
    	case CobraRadarMessageNotification.TYPE_NOTIFY:
        	RadarMessageNotification msgNotify = new RadarMessageNotification();
        	msgNotify.message = msg.message;
        	onRadarMessage(msgNotify);
        	break;
    	default:
    	}
    }
    
    public final void onEventAsync(CobraRadarMessageAlert alertMsg) {
    	RadarMessageThreat msgThreat = new RadarMessageThreat(alertMsg.alert, alertMsg.strength, alertMsg.frequency);
    	isThreatActive = true;
    	if ( alertMsg.minAlerTime != null ) {
    		isThreatForcedActive.set(true);
    		if ( alertTimer != null )
    			alertTimer.cancel();
    		alertTimer = new Timer();
    		alertTimer.schedule(new TimerTask() {
				@Override
				public void run() {
		    		isThreatForcedActive.set(false);
		    		eventBus.post(new CobraRadarMessageStopAlert(batteryVoltage));
				}
			}, alertMsg.minAlerTime);
    	}
    	onRadarMessage(msgThreat);
    }
    
    
    public final void onEventAsync(CobraRadarMessageStopAlert stopAlertMsg) {
    	RadarMessageAllClear msgClear = new RadarMessageAllClear();
    	this.batteryVoltage = stopAlertMsg.batteryVoltage;
    	// Send "All Clear" message only if threats are active, and not forcibly held active 
    	if ( isThreatActive && !isThreatForcedActive.get() ) {
    		onRadarMessage(msgClear);
    		isThreatActive = false;
    	}
    }
    
    public final Double getBatteryVoltage() {
    	return batteryVoltage;
    }
    
    public final ConnectivityStatus getConnStatus() {
    	return connStatus;
    }
    
    public abstract void onRadarMessage(RadarMessage msg);
	
}
