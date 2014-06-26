package com.cobra.iradar;

import com.greatnowhere.radar.messaging.ConnectivityStatus;

import de.greenrobot.event.EventBus;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * A service handling connection to iRadar
 * Communicates with any bound callers via messages
 * @author pzeltins
 *
 */
public class RadarConnectionService extends Service {

	private static final String TAG = RadarConnectionService.class.getCanonicalName();
	public static final int NOTIFICATION_CONNECTED = 1;
	
	private BluetoothDevice iRadarDevice;
	private RadarConnectionThread radarThread;
    private Notification connectedNotification;
    private EventBus eventBus = EventBus.getDefault();
	
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
    	super.onStartCommand(intent,flags,startId);
    	
    	Log.i(TAG, "Service Start");
    	
    	if ( intent != null && intent.getParcelableExtra(RadarConnectionServiceIntent.RADAR_DEVICE) != null ) {
    		iRadarDevice = intent.getParcelableExtra(RadarConnectionServiceIntent.RADAR_DEVICE);
    	}
		connectedNotification = RadarManager.getOngoingNotification();
    	if ( iRadarDevice != null )
    		runConnection();
    	
    	if ( !eventBus.isRegistered(this) ) 
    		eventBus.register(this);
    	
    	return START_STICKY;
    }
	
	@Override
	public void onDestroy() {
    	Log.i(TAG, "Service Stop");
		super.onDestroy();
		setNotification();
    	stopConnection();
    	if ( eventBus.isRegistered(this) )
    		eventBus.unregister(this);
	}
	
	public synchronized void runConnection() {
		// cant run thread unless device is defined
		if ( iRadarDevice == null )
			return;
		
    	if ( radarThread == null || !RadarConnectionThread.isRunning.get() ) {
    		Log.i(TAG, "Starting new BT connection thread");
	    	radarThread = new RadarConnectionThread(iRadarDevice);
	    	radarThread.start();
    	} else {
    		Log.i(TAG, "Not starting new BT connection thread: already running");
    	}
	}
	
	private synchronized void stopConnection() {
    	if ( radarThread != null && radarThread.isAlive() ) {
    		radarThread.interrupt();
    	}
	}
	
	private void setNotification() {
		if ( connectedNotification != null && RadarManager.getConnectivityStatus() == ConnectivityStatus.CONNECTED ) {
			startForeground(NOTIFICATION_CONNECTED, connectedNotification);
		} else {
			stopForeground(true);
		}
	}

	/**
	 * Not intended to be bound
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void onEventAsync(CobraRadarEvents.EventDeviceConnected event) {
		setNotification();
	}

	public void onEventAsync(CobraRadarEvents.EventDeviceDisconnected event) {
		setNotification();
		stopSelf();
	}
	
}
