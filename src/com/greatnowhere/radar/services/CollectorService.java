package com.greatnowhere.radar.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

import com.cobra.iradar.RadarManager;
import com.cobra.iradar.RadarScanManager;
import com.greatnowhere.radar.MainRadarActivity;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.location.LocationInfoLookupManager;
import com.greatnowhere.radar.location.PhoneActivityDetector;
import com.greatnowhere.radar.location.RadarLocationManager;
import com.greatnowhere.radar.messaging.ConnectivityStatus;
import com.greatnowhere.radar.messaging.RadarMessageAllClear;
import com.greatnowhere.radar.messaging.RadarMessageConnectivityNotification;
import com.greatnowhere.radar.messaging.RadarMessageNotification;
import com.greatnowhere.radar.messaging.RadarMessageThreat;
import com.greatnowhere.radar.threats.AlertAudioManager;
import com.greatnowhere.radar.threats.TTSManager;
import com.greatnowhere.radar.threats.ThreatManager;
import com.greatnowhere.radar.util.NotificationBuilder;

import de.greenrobot.event.EventBus;

/**
 * Collects data from radar, logs it, displays system popups, and provides data to display for the main activity
 * @author pzeltins
 *
 */
public class CollectorService extends Service {

	private static final String TAG = CollectorService.class.getCanonicalName();
	
	public static final String INTENT_KEY_MANUAL_RECONNECT = "attemptReconnect";
	
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
    	
    	if ( !isRadarInitialized ) {
    		init();
    	}
    	
    	if ( intent != null && intent.hasExtra(RadarManager.INTENT_ACTIVITY_EXTRA_KEY_MSG) && isRadarInitialized ) {
    		Serializable s = intent.getSerializableExtra(RadarManager.INTENT_ACTIVITY_EXTRA_KEY_MSG);
    		Log.i(TAG, "Collector received Cobra message " + s.toString());
    		eventBus.post(s);
    	}
    	
    	if ( intent != null && intent.hasExtra(INTENT_KEY_MANUAL_RECONNECT) ) { 
    		RadarScanManager.scanForced();
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
	    NotificationBuilder.init(getApplicationContext());
	    
	    // Webservices client init
	    if ( Preferences.isLookupSpeedLimit() )
	    	LocationInfoLookupManager.init(getApplicationContext());
	    
    	isRadarInitialized = RadarManager.initialize(getApplicationContext(), 
    			NotificationBuilder.getConnectedNotification(), NotificationBuilder.getScanNotification(), 
				Preferences.isScanForDevice(), Preferences.getDeviceScanInterval(), CollectorService.class);
    	
	    
	    if ( !isRadarInitialized ) {
	    	addLogMessage("Unable to initialize: BT not active, or device not paired");
	    } else {
		    // Scan manager, must be initialized after RadarManager 
		    RadarScanner.init();
	    }
	    
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
        RadarScanner.stop();
        LocationInfoLookupManager.stop();
        radarMessageHandler.unRegister();
        isRadarInitialized = false;
        eventBus = null;
    }
    
	public IBinder onBind(Intent intent) {
		return null;
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
		if ( instance != null )
			return instance.screenLogString;
		return "";
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
