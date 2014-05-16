package com.greatnowhere.radar.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import com.cobra.iradar.protocol.CobraRadarMessageNotification;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.messaging.ConnectivityStatus;

import de.greenrobot.event.EventBus;

public class RadarLocationManager {

	private static Context ctx;
	private static LocationManager lm;
	private static Location currentLoc;
	private static LocationListener locListener;
	private static boolean isReady = false;
	private static EventBus eventBus;
	
	public static void init(Context ctx) {
		RadarLocationManager.ctx = ctx;
		eventBus = EventBus.getDefault();
		eventBus.register(new ConnectivityEventsListener());
		lm = (LocationManager) RadarLocationManager.ctx.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public static void init() {
		if ( ctx == null )
			return;
		
		if ( Preferences.isLogThreatLocation() ) {
			start();
		} else {
			stop();
		}
	}
	
	public static void start() {
		locListener = new LocationListener();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100L, 1L, locListener);
	}
	
	public static void stop() {
		if ( locListener != null ) {
			lm.removeUpdates(locListener);
			locListener = null;
		}
		isReady = false;
	}
	
	public static Location getCurrentLoc() {
		return currentLoc;
	}

	public static void setCurrentLoc(Location currentLoc) {
		RadarLocationManager.currentLoc = currentLoc;
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
	
	private static class ConnectivityEventsListener {
		@SuppressWarnings("unused")
		public void onEventMainThread(CobraRadarMessageNotification msg) {
			if ( msg.type == CobraRadarMessageNotification.TYPE_CONN ) {
				switch (ConnectivityStatus.fromCode(msg.connectionStatus)) {
				case CONNECTED:
					init();
					break;
				case DISCONNECTED:
					stop();
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
	
}
