package com.cobra.iradar;

import java.util.Random;

import de.greenrobot.event.EventBus;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;

/**
 * A service handling connection to iRadar
 * Communicates with any bound callers via messages
 * @author pzeltins
 *
 */
public class RadarConnectionService extends Service {

	private BluetoothDevice iRadarDevice;
	private RadarConnectionThread radarThread;
    private int notificationId = new Random().nextInt();
    private EventBus eventBus = EventBus.getDefault();
	
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
    	int retVal = super.onStartCommand(intent,flags,startId);
    	if ( intent.getParcelableExtra(RadarConnectionServiceIntent.RADAR_DEVICE) != null ) {
    		iRadarDevice = intent.getParcelableExtra(RadarConnectionServiceIntent.RADAR_DEVICE);
    		Notification fgNotify = intent.getParcelableExtra(RadarConnectionServiceIntent.RADAR_NOTIFICATION);
        	if ( IRadarManager.isShowNotification() && fgNotify != null ) {
        		startForeground(notificationId, fgNotify );
        	}
        	runConnection();
    	}
    	if ( !eventBus.isRegistered(this) )
    		eventBus.register(this);
    	return retVal;
    }
	
	@Override
	public void onDestroy() {
		super.onDestroy();
    	if ( IRadarManager.isShowNotification() ) {
    		stopForeground(true);
    	}
    	stopConnection();
	}
	
	public void runConnection() {
    	if ( iRadarDevice != null && ( radarThread == null || !radarThread.isAlive()) ) {
	    	radarThread = new RadarConnectionThread(iRadarDevice);
	    	radarThread.start();
    	}
	}
	
	private void stopConnection() {
    	if ( radarThread != null && radarThread.isAlive() ) {
    		radarThread.interrupt();
    	}
	}

	/**
	 * Not intended to be bound
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void onEventMainThread(EventReconnectioAttempt event) {
		runConnection();
	}

	/**
	 * Empty class, used by event bus to signal reconnection attempt
	 * @author pzeltins
	 *
	 */
	public static class EventReconnectioAttempt {
	}
	
}
