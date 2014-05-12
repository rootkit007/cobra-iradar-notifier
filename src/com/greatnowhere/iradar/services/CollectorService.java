package com.greatnowhere.iradar.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

import com.cobra.iradar.IRadarMessageHandler;
import com.cobra.iradar.RadarManager;
import com.cobra.iradar.RadarScanManager;
import com.cobra.iradar.messaging.CobraMessageAllClear;
import com.cobra.iradar.messaging.CobraMessageConnectivityNotification;
import com.cobra.iradar.messaging.CobraMessageNotification;
import com.cobra.iradar.messaging.CobraMessageThreat;
import com.cobra.iradar.messaging.ConnectivityStatus;
import com.greatnowhere.iradar.MainRadarActivity;
import com.greatnowhere.iradar.R;
import com.greatnowhere.iradar.config.Preferences;
import com.greatnowhere.iradar.location.LocationManager;
import com.greatnowhere.iradar.threats.AlertAudioManager;
import com.greatnowhere.iradar.threats.TTSManager;
import com.greatnowhere.iradar.threats.ThreatManager;

import de.greenrobot.event.EventBus;

/**
 * Collects data from radar, logs it, displays system popups, and provides data to display for the main activity
 * @author pzeltins
 *
 */
public class CollectorService extends Service {

	private static final String TAG = CollectorService.class.getCanonicalName();
	
    private EventBus eventBus = EventBus.getDefault();
    private boolean isRadarInitialized = false;
    
    private static Queue<String> screenLog = new ConcurrentLinkedQueue<String>();
    private static ConnectivityStatus connStatus = ConnectivityStatus.UNKNOWN;
    private static String lastAlert = "";
    
    public int onStartCommand(Intent intent, int flags, int startId) {
    	int retVal = super.onStartCommand(intent, flags, startId);
    	
    	if ( !eventBus.isRegistered(this)) {
    		eventBus.register(this);
    	}
    	
    	init();
    	
	    return retVal;
    }
    
    private void init() {
	    // Initialize alerts audio manager
	    AlertAudioManager.init(getApplicationContext());
	    
	    // initialize threats manager
	    ThreatManager.init(getApplicationContext());
	    
	    // Initialize TTS 
	    TTSManager.init(getApplicationContext());

	    // ongoing notifications
    	Builder b;
    	Notification scanNotification = null;
    	Notification connectedNotification = null;
    	Intent resumeAppIntent;
    	if ( Preferences.isNotifyOngoingScan() ) {
    		b = new Notification.Builder(getApplicationContext());
    		b.setContentText("iRadar Scanning For Devices");
    		resumeAppIntent = new Intent(getApplicationContext(), MainRadarActivity.class);
    		resumeAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
    		b.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, resumeAppIntent , 0));
    		b.setContentTitle("iRadar Notifier");
    		b.setOngoing(true);
    		b.setSmallIcon(R.drawable.app_icon);
    		scanNotification = b.build();
    	}
    	if ( Preferences.isNotifyOngoingConnected() ) {
			b = new Notification.Builder(getApplicationContext());
			b.setContentText("iRadar Notifier Connected");
			resumeAppIntent = new Intent(getApplicationContext(), MainRadarActivity.class);
			resumeAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
			b.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, resumeAppIntent , 0));
			b.setContentTitle("iRadar Notifier");
    		b.setOngoing(true);
			b.setSmallIcon(R.drawable.app_icon);
			connectedNotification = b.build();
    	}
    	isRadarInitialized = RadarManager.initialize(getApplicationContext(), Preferences.isNotifyOngoingConnected(),
				connectedNotification, scanNotification, 
				Preferences.isScanForDevice(), Preferences.getDeviceScanInterval(), Preferences.isScanForDeviceInCarModeOnly());
    	if ( !isRadarInitialized ) {
    		connStatus = ConnectivityStatus.PROTOCOL_ERROR;
    	}
    	
    }
    
    @Override
    public void onDestroy() {
    	Log.d(TAG, "onDestroy");
    	super.onDestroy();
        RadarManager.stop();
        TTSManager.stop();
        ThreatManager.stop();
        LocationManager.stop();
        radarMessageHandler.stop();
        eventBus.unregister(this);
    }
    
	public IBinder onBind(Intent intent) {
		return null;
	}
	
    public void onEvent(Preferences.PreferenceScanChangedEvent event) {
    	RadarScanManager.scan(Preferences.isScanForDevice(), Preferences.getDeviceScanInterval(), Preferences.isScanForDeviceInCarModeOnly());
    }

    // The Handler that gets information back from the IRadar
    // All event handlers are called in async thread, so care must be taken when updating UI
	private static IRadarMessageHandler radarMessageHandler = new IRadarMessageHandler() {

		@Override
		public void onRadarMessage(final CobraMessageConnectivityNotification msg) {
				addLogMessage(msg.message);
				CollectorService.connStatus = msg.status;
				if ( CollectorService.connStatus != ConnectivityStatus.CONNECTED ) {
					
				}
		}

		@Override
		public void onRadarMessage(final CobraMessageThreat msg) {
	        	addLogMessage("Alert " + msg.alertType.getName() + msg.strength + " " + msg.frequency);
	        	lastAlert = msg.alertType.getName() + " " + msg.frequency;
	        	ThreatManager.newThreat(msg);
		}

		@Override
		public void onRadarMessage(final CobraMessageAllClear msg) {
				lastAlert = "";
				ThreatManager.removeThreats();
		}

		@Override
		public void onRadarMessage(final CobraMessageNotification msg) {
				addLogMessage(msg.message);
		}
	};
	
    public synchronized static String getConnStatus() {
		return connStatus.getStatusName();
	}

	public synchronized static String getBatteryVoltage() {
		return (connStatus == ConnectivityStatus.CONNECTED ? Double.toString(radarMessageHandler.getBatteryVoltage()) : "" );
	}

	public synchronized static String getLastAlert() {
		return lastAlert;
	}

	public synchronized static String getLog() {
		String log = "";
		for ( String s : screenLog ) {
			log = s + "\n" + log;
		}
		return log;
	}

	public static void addLogMessage(String msg) {
    	// generate timestamp
    	String tsMsg = DateFormat.format("M/d HH:mm:ss", new Date()).toString() + " " + msg;
    	screenLog.add(tsMsg);
    	if ( screenLog.size() > Preferences.getScreenLogScrollBackLimit() )
    		screenLog.remove();
    	//log.setText(tsMsg + log.getText());
    	addFileLogMessage(tsMsg);
    }
    
    public static void addFileLogMessage(String msg) {
    	if ( Preferences.getLogFileName().isEmpty() )
    		return;
    	try {
    		  File logFile = new File(
    				  Environment.getExternalStorageDirectory(), 
    				  Preferences.getLogFileName());
    		  OutputStream os = new FileOutputStream(logFile, true);
    		  os.write(msg.getBytes());
    		  os.close();
    	} catch (Exception e) {
    		  e.printStackTrace();
    	}
    }
    
}
