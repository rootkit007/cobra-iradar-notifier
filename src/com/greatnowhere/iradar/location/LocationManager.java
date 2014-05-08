package com.greatnowhere.iradar.location;

import com.cobra.iradar.messaging.ConnectivityStatus;
import com.cobra.iradar.protocol.RadarMessageNotification;
import com.greatnowhere.iradar.config.Preferences;

import de.greenrobot.event.EventBus;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

public class LocationManager {

	private static Context ctx;
	private static android.location.LocationManager lm;
	private static Location currentLoc;
	private static LocationListener locListener;
	private static boolean isReady = false;
	private static EventBus eventBus = EventBus.getDefault();
	
	public static void init(Context ctx) {
		LocationManager.ctx = ctx;
		eventBus.register(new ConnectivityEventsListener());
		lm = (android.location.LocationManager) LocationManager.ctx.getSystemService(Context.LOCATION_SERVICE);
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
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setSpeedRequired(true);
		c.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
		locListener = new LocationListener();
		lm.requestLocationUpdates(100L, 10L, c, locListener, null);
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
		LocationManager.currentLoc = currentLoc;
		isReady = true;
	}

	public static boolean isReady() {
		return isReady;
	}

	public static void setReady(boolean isReady) {
		LocationManager.isReady = isReady;
	}
	
	private static class ConnectivityEventsListener {
		@SuppressWarnings("unused")
		public void onEventMainThread(RadarMessageNotification msg) {
			if ( msg.type == RadarMessageNotification.TYPE_CONN ) {
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

		@Override
		public void onLocationChanged(Location location) {
			setCurrentLoc(location);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

	}
	
}
