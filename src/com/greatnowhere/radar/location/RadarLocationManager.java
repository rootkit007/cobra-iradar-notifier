package com.greatnowhere.radar.location;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.cobra.iradar.CobraRadarEvents;
import com.cobra.iradar.RadarManager;
import com.greatnowhere.radar.config.Preferences;

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
	private static RadarLocationManager instance;
	
	public static void init(Context ctx) {
		RadarLocationManager.ctx = ctx;
		eventBus = EventBus.getDefault();
		instance = new RadarLocationManager();
		eventBus.register(instance);
		lm = (LocationManager) RadarLocationManager.ctx.getSystemService(Context.LOCATION_SERVICE);
		instance.activate();
	}
	
	private boolean isShouldActivate() {
		return ( ( Preferences.isLogThreatLocation() && RadarManager.isRadarConnected() ) ||
				( LocationInfoLookupManager.isShouldActivate() ) );
	}
	
	private synchronized void activate() {
		Log.d(TAG,"activate()");
		if ( isShouldActivate() && !isActive.get() ) {
			start();
		} else if ( !isShouldActivate() && isActive.get() ){
			stop();
		}
	}
	
	public synchronized static void start() {
		Log.i(TAG,"start");
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
		isReady = false;
		isActive.set(false);
	}
	
	public static void destroy() {
		stop();
		if ( eventBus != null && instance != null && eventBus.isRegistered(instance) )
			eventBus.unregister(instance);
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
	public void onEventMainThread(Preferences.PreferenceLocationLookupSettingsChangedEvent event) {
		activate();
	}
	
	public void onEventMainThread(Preferences.PreferenceOverSpeedSettingsChangedEvent event) {
		activate();
	}

	public void onEventMainThread(CobraRadarEvents.EventDeviceConnected event) {
		activate();
	}

	public void onEventMainThread(CobraRadarEvents.EventDeviceDisconnected event) {
		activate();
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
