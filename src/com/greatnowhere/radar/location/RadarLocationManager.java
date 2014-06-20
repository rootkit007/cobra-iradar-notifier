package com.greatnowhere.radar.location;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.cobra.iradar.RadarManager;
import com.cobra.iradar.protocol.CobraRadarMessageNotification;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.messaging.ConnectivityStatus;

import de.greenrobot.event.EventBus;

public class RadarLocationManager {

	private static final String TAG = RadarLocationManager.class.getCanonicalName();
	
	private static Context ctx;
	private static LocationManager lm;
	private static Location currentLoc;
	private static LocationListener locListener;
	private static boolean isReady = false;
	private static EventBus eventBus;
	private static AtomicBoolean isActive = new AtomicBoolean(false);
	private static ConnectivityEventsListener connectivityListener;
	
	public static void init(Context ctx) {
		RadarLocationManager.ctx = ctx;
		eventBus = EventBus.getDefault();
		connectivityListener = new ConnectivityEventsListener();
		eventBus.register(connectivityListener);
		lm = (LocationManager) RadarLocationManager.ctx.getSystemService(Context.LOCATION_SERVICE);
		activate();
	}
	
	private static boolean isShouldActivate() {
		return ( Preferences.isLogThreatLocation() && RadarManager.getConnectivityStatus() == ConnectivityStatus.CONNECTED ) ||
				( LocationInfoLookupManager.isShouldActivate() );
	}
	
	private synchronized static void activate() {
		if ( isShouldActivate() && !isActive.get() ) {
			start();
		} else if ( !isShouldActivate() && isActive.get() ){
			stop();
		}
	}
	
	public synchronized static void start() {
		locListener = new LocationListener();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100L, 1L, locListener);
		isActive.set(true);
	}
	
	public synchronized static void stop() {
    	Log.d(TAG, "stop");
		if ( locListener != null ) {
			lm.removeUpdates(locListener);
			locListener = null;
		}
		if ( connectivityListener != null && eventBus.isRegistered(connectivityListener) ) {
			eventBus.unregister(connectivityListener);
			connectivityListener = null;
		}
		isReady = false;
		isActive.set(false);
	}
	
	public static Location getCurrentLoc() {
		return currentLoc;
	}

	public static void setCurrentLoc(Location currentLoc) {
		RadarLocationManager.currentLoc = currentLoc;
		eventBus.postSticky(new LocationChanged(currentLoc));
		isReady = true;
	}
	
	public static float getCurrentSpeedKph() {
		if ( getCurrentLoc() != null ) {
			return getCurrentLoc().getSpeed()*3.6f;
		}
		return 0f;
	}
	
	public static float getCurrentSpeedMph() {
		if ( getCurrentLoc() != null ) {
			return getCurrentLoc().getSpeed()*2.23694f;
		}
		return 0f;
	}

	public static boolean isReady() {
		return isReady;
	}

	public static void setReady(boolean isReady) {
		RadarLocationManager.isReady = isReady;
	}
	
	/**
	 * Event handlers to start/stop lookup
	 * @param event
	 */
	public void onEventAsync(PhoneActivityDetector.EventActivityChanged event) {
		activate();
	}

	public void onEventAsync(PhoneActivityDetector.EventCarModeChange event) {
		activate();
	}
	
	public void onEventAsync(Preferences.PreferenceLocationLookupSettingsChangedEvent event) {
		activate();
	}
	
	public void onEventAsync(Preferences.PreferenceOverSpeedSettingsChangedEvent event) {
		activate();
	}
	
	private static class ConnectivityEventsListener {
		@SuppressWarnings("unused")
		public void onEventMainThread(CobraRadarMessageNotification msg) {
			if ( msg.type == CobraRadarMessageNotification.TYPE_CONN ) {
				switch (ConnectivityStatus.fromCode(msg.connectionStatus)) {
				case CONNECTED:
					activate();
					break;
				case DISCONNECTED:
					activate();
					break;
				default:
				}
				
			}
		}
	}

	private static class LocationListener implements android.location.LocationListener {
		public void onLocationChanged(Location location) {
			setCurrentLoc(location);
		}
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		public void onProviderEnabled(String provider) {
		}
		public void onProviderDisabled(String provider) {
		}

	}
	
	public static class LocationChanged {
		public Location loc;
		
		public LocationChanged(Location l) {
			loc = l;
		}

		public float getCurrentSpeedKph() {
			if ( loc != null ) {
				return loc.getSpeed()*3.6f;
			}
			return 0f;
		}
		
		public float getCurrentSpeedMph() {
			if ( loc != null ) {
				return loc.getSpeed()*2.23694f;
			}
			return 0f;
		}

	
	}
	
}
