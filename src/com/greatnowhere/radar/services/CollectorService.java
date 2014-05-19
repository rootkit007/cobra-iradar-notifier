package com.greatnowhere.radar.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
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

import com.cobra.iradar.RadarManager;
import com.cobra.iradar.RadarScanManager;
import com.greatnowhere.radar.R;
import com.greatnowhere.radar.MainRadarActivity;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.location.PhoneActivityDetector;
import com.greatnowhere.radar.location.RadarLocationManager;
import com.greatnowhere.radar.messaging.RadarMessageAllClear;
import com.greatnowhere.radar.messaging.RadarMessageConnectivityNotification;
import com.greatnowhere.radar.messaging.RadarMessageNotification;
import com.greatnowhere.radar.messaging.RadarMessageThreat;
import com.greatnowhere.radar.messaging.ConnectivityStatus;
import com.greatnowhere.radar.threats.AlertAudioManager;
import com.greatnowhere.radar.threats.TTSManager;
import com.greatnowhere.radar.threats.ThreatManager;

import de.greenrobot.event.EventBus;

/**
 * Collects data from radar, logs it, displays system popups, and provides data to display for the main activity
 * @author pzeltins
 *
 */
public class CollectorService extends Service {

	private static final String TAG = CollectorService.class.getCanonicalName();
	
    private EventBus eventBus;
	private boolean isRadarInitialized = false;
    
    private Queue<String> screenLog = new ConcurrentLinkedQueue<String>();
    private String screenLogString = "";
    
    private static CollectorService instance;
    
    public int onStartCommand(Intent intent, int flags, int startId) {
    	super.onStartCommand(intent, flags, startId);
    	
    	if ( eventBus == null )
    		eventBus = EventBus.getDefault();
    	
    	instance = this;
    	
    	if ( intent != null && intent.hasExtra(RadarManager.INTENT_ACTIVITY_EXTRA_KEY_MSG) && isRadarInitialized ) {
    		Serializable s = intent.getSerializableExtra(RadarManager.INTENT_ACTIVITY_EXTRA_KEY_MSG);
    		Log.i(TAG, "Posting Cobra message " + s.toString());
    		eventBus.post(s);
    	} else {
    		init();
    	}
    	
	    return START_STICKY;
    }
    
    private void init() {

    	// Initialize preferences
    	Preferences.init(getApplicationContext());
    	
	    // Initialize alerts audio manager
	    AlertAudioManager.init(getApplicationContext());
	    
	    // initialize threats manager
	    ThreatManager.init(getApplicationContext());
	    
	    // Initialize TTS 
	    TTSManager.init(getApplicationContext());

	    // Initialize activity detector
	    PhoneActivityDetector.init(getApplicationContext());
	    
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
				Preferences.isScanForDevice(), Preferences.getDeviceScanInterval(), Preferences.isScanForDeviceInCarModeOnly(),
				CollectorService.class);
    	
    }
    
    @Override
    public void onDestroy() {
    	Log.d(TAG, "onDestroy");
    	super.onDestroy();
        RadarManager.stop();
        TTSManager.stop();
        ThreatManager.stop();
	    PhoneActivityDetector.stop();
        RadarLocationManager.stop();
        radarMessageHandler.stop();
        eventBus = null;
    }
    
	public IBinder onBind(Intent intent) {
		return null;
	}
	
    public void onEvent(Preferences.PreferenceScanChangedEvent event) {
    	RadarScanManager.scan(Preferences.isScanForDevice(), Preferences.getDeviceScanInterval(), Preferences.isScanForDeviceInCarModeOnly());
    }

    public synchronized static String getConnStatus() {
		return RadarManager.getConnectivityStatus().getStatusName();
	}

	public synchronized static String getBatteryVoltage() {
		return (RadarManager.getConnectivityStatus() == ConnectivityStatus.CONNECTED ? Double.toString(RadarManager.getBatteryVoltage()) : "" );
	}

	public synchronized static String getCurrentAlert() {
    	if ( instance == null )
    		return null;
		return ThreatManager.getCurrentThreat();
	}

	public synchronized static String getLog() {
		return instance.screenLogString;
	}
	
	private synchronized static String getLogAsString() {
    	if ( instance == null )
    		return null;
		String log = "";
		for ( String s : instance.screenLog ) {
			log = s + "\n" + log;
		}
		return log;
	}

	private static void addLogMessage(String msg) {
		if ( instance == null )
			return;
    	// generate timestamp
    	String tsMsg = DateFormat.format("M/d HH:mm:ss", new Date()).toString() + " " + msg;
    	instance.screenLog.add(tsMsg);
    	if ( instance.screenLog.size() > Preferences.getScreenLogScrollBackLimit() )
    		instance.screenLog.remove();
    	instance.screenLogString = getLogAsString();
    	//log.setText(tsMsg + log.getText());
    	addFileLogMessage(tsMsg);
    	instance.eventBus.post(new MainRadarActivity.UIRefreshLogEvent());
    }
    
    private static void addFileLogMessage(String msg) {
    	if ( Preferences.getLogFileName().isEmpty() )
    		return;
    	try {
    		  File logFile = new File(
    				  Environment.getExternalStorageDirectory(), 
    				  Preferences.getLogFileName());
    		  OutputStream os = new FileOutputStream(logFile, true);
    		  os.write((msg + "\n").getBytes());
    		  os.close();
    	} catch (Exception e) {
    		  e.printStackTrace();
    	}
    }
    
    // The Handler that gets information back from the IRadar
    // All event handlers are called in background thread, so care must be taken when updating UI
	private CobraMessageHandler radarMessageHandler = new CobraMessageHandler() {
		@Override
		public void onEventBackgroundThread(final RadarMessageConnectivityNotification msg) {
			// nothing to do here. CobraMessageConnectivityNotification is a subclass of
			// CobraMessageNotification so this message will get logged there
			// and actual connectivity changes are handled elsewhere
		}
		
		@Override
		public void onEventBackgroundThread(final RadarMessageThreat msg) {
	        	ThreatManager.newThreat(msg);
		}
		@Override
		public void onEventBackgroundThread(final RadarMessageAllClear msg) {
				ThreatManager.removeThreats();
		}
		@Override
		public void onEventBackgroundThread(final RadarMessageNotification msg) {
				addLogMessage(msg.message);
		}
	};
	
}
