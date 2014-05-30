package com.greatnowhere.radar.location;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.cobra.iradar.protocol.CobraRadarMessageNotification;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.greatnowhere.radar.messaging.RadarMessageNotification;

import de.greenrobot.event.EventBus;

public class PhoneActivityDetector implements GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	private static final String TAG = PhoneActivityDetector.class.getCanonicalName();
	
	private static PhoneActivityDetector instance;
	private static ActivityRecognitionClient activityClient;
	private static ActivityStatus activity = ActivityStatus.UNKNOWN;
	private static Context ctx;
	private static final long ACTIVITY_UPDATE_INTERVAL = 60000L; // every 60 secs
	private static UiModeManager uiManager;
	private static AtomicBoolean isCarMode = new AtomicBoolean();
	private static EventBus eventBus = EventBus.getDefault();
	
	public static void init(Context ctx) {
		Log.d(TAG,"init");
		instance = new PhoneActivityDetector();
		PhoneActivityDetector.ctx = ctx;
		eventBus = EventBus.getDefault();
		uiManager = (UiModeManager) ctx.getSystemService(Context.UI_MODE_SERVICE);
		isCarMode.set( uiManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR );

		int gpsResultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(ctx);
		if ( gpsResultCode == ConnectionResult.SUCCESS ) {
			activityClient = new ActivityRecognitionClient(ctx, instance, instance);
			activityClient.connect();
		} else {
			activityClient = null;
			setActivityStatus(ActivityStatus.UNAVAILABLE);
			eventBus.post(new RadarMessageNotification("Activity detection not available!\nError code " + gpsResultCode));
		}

	}
	
	public static void stop() {
		Log.d(TAG,"stop");
		if ( activityClient.isConnected() )
			activityClient.disconnect();
	}

	public static ActivityStatus getActivityStatus() {
		synchronized (activity) {
			return activity;
		}
	}

	private static void setActivityStatus(ActivityStatus a) {
		synchronized (activity) {
			if ( a != activity ) {
				activity = a;
				eventBus.postSticky(new EventActivityChanged(a));
			}
		}
	}
	
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(TAG,"connection failed");
		setActivityStatus(ActivityStatus.UNKNOWN);
	}


	public void onConnected(Bundle connectionHint) {
		Log.d(TAG,"connected");
		Intent i = new Intent(ctx, ActivityDetectorIntentReceiver.class);
		PendingIntent callbackIntent = PendingIntent.getService(ctx, 0, i,
	             PendingIntent.FLAG_UPDATE_CURRENT);
		activityClient.requestActivityUpdates(ACTIVITY_UPDATE_INTERVAL, callbackIntent);
	}


	public void onDisconnected() {
		Log.d(TAG,"disconnected");
		setActivityStatus(ActivityStatus.UNKNOWN);
	}

	public static boolean getIsCarMode() {
		return isCarMode.get();
	}

	public static void setIsCarMode(boolean isCarMode) {
		if (isCarMode != PhoneActivityDetector.isCarMode.get() )
			eventBus.post(new CobraRadarMessageNotification("Car mode " + (isCarMode ? "activated" : "deactivated")));
		PhoneActivityDetector.isCarMode.set(isCarMode);
		eventBus.post(new EventCarModeChange());
	}

	public enum ActivityStatus {
		DRIVING("Driving",DetectedActivity.IN_VEHICLE),STILL("Still",DetectedActivity.STILL),
		FOOT("Walking",DetectedActivity.ON_FOOT),BICYCLE("Bicycling",DetectedActivity.ON_BICYCLE),
		UNKNOWN("Unknown",DetectedActivity.UNKNOWN), UNAVAILABLE("Service Unavailable",-1);

		private ActivityStatus(String n,int c) {
			code=c;
			name=n;
		}
		
		private int code;
		private String name;
		
		public int getCode() {
			return code;
		}
		public String getName() {
			return name;
		}
		
		public static ActivityStatus fromDetectedActivity(DetectedActivity act) {
			for (ActivityStatus s : ActivityStatus.values()) {
				if (s.getCode() == act.getType()) {
					return s;
				}
			}
			// Exception for TILTING - useless activity anyway
			if ( act.getType() == DetectedActivity.TILTING )
				return ActivityStatus.STILL;
			
			return UNKNOWN;
		}
	}
	
	public static class ActivityDetectorIntentReceiver extends IntentService {
		
		private static final String TAG = ActivityDetectorIntentReceiver.class.getCanonicalName();

		public ActivityDetectorIntentReceiver() {
			super(PhoneActivityDetector.class.getCanonicalName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			Log.d(TAG,"onHandleIntent");
			if (ActivityRecognitionResult.hasResult(intent)) {
		         ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
		         if ( result != null ) {
			         Log.d(TAG,"Got activity update " + result.getMostProbableActivity());
			         ActivityStatus newActivity = ActivityStatus.fromDetectedActivity(result.getMostProbableActivity());
			         setActivityStatus(newActivity);
		         }
		    }
		}
	}

	/**
	 * Event to be dispatched on event bus whenever activity status changes
	 * @author pzeltins
	 *
	 */
	public static class EventActivityChanged {
		
		public EventActivityChanged() {
		}
		
		public EventActivityChanged(ActivityStatus act) {
			this();
			activity = act;
		}
		
		public ActivityStatus activity;
	}
	
	public static class EventCarModeChange {
		
	}
}
