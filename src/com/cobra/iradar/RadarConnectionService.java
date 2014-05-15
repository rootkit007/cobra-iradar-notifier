package com.cobra.iradar;

import com.cobra.iradar.messaging.ConnectivityStatus;
import com.cobra.iradar.protocol.RadarMessageNotification;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import de.greenrobot.event.EventBus;

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
    @SuppressWarnings("unused")
	private int notificationId = 1;
    private EventBus eventBus;
    private Notification connectedNotification;
	
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
    	super.onStartCommand(intent,flags,startId);
    	
    	Log.i(TAG, "Service Start");
    	
		eventBus = EventBus.getDefault();
		if ( !eventBus.isRegistered(this) )
    		eventBus.register(this);
    	if ( intent != null && intent.getParcelableExtra(RadarConnectionServiceIntent.RADAR_DEVICE) != null ) {
    		iRadarDevice = intent.getParcelableExtra(RadarConnectionServiceIntent.RADAR_DEVICE);
    		connectedNotification = intent.getParcelableExtra(RadarConnectionServiceIntent.RADAR_NOTIFICATION);
    	}
    	if ( iRadarDevice != null )
    		runConnection();
    	return START_STICKY;
    }
	
	@Override
	public void onDestroy() {
    	Log.i(TAG, "Service Stop");
		super.onDestroy();
    	if ( RadarManager.isShowNotification() ) {
    		stopForeground(true);
    	}
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

	/**
	 * Not intended to be bound
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void onEventAsync(RadarMessageNotification msg) {
		if ( msg.type == RadarMessageNotification.TYPE_CONN ) {
			switch (ConnectivityStatus.fromCode(msg.connectionStatus)) {
			case DISCONNECTED:
				stopForeground(true);
				stopSelf();
				break;
			case CONNECTED:
	        	if ( connectedNotification != null ) {
	        		startForeground(NOTIFICATION_CONNECTED, connectedNotification );
	        	}
				break;
			default:
			}
		}
	}

}
