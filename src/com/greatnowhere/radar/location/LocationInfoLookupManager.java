package com.greatnowhere.radar.location;

import com.greatnowhere.osmclient.OSMLocationListener;
import com.greatnowhere.osmclient.OSMLocationListener.OSMWayChangedListener;
import com.greatnowhere.radar.messaging.RadarMessageNotification;
import com.xapi.models.Way;

import de.greenrobot.event.EventBus;
import android.content.Context;
import android.util.Log;

public class LocationInfoLookupManager {

	private static final String TAG = LocationInfoLookupManager.class.getCanonicalName();
	
	private static OSMLocationListener osmListener;
	private static EventBus eventBus;
	private static Way currentWay;
	
	public static void init(Context ctx) {
		osmListener = new OSMLocationListener(ctx);
		osmListener.setOSMWayListener(new OSMListener());
		eventBus = EventBus.getDefault();
	}
	
	public static void stop() {
		if ( osmListener != null )
			osmListener.stop();
		osmListener = null;
	}

	public static String getCurrentWayName() {
		if ( currentWay != null )
			return currentWay.getRoadName();
		return null;
	}
	
	public static Integer getCurrentSpeedLimit() {
		if ( currentWay != null ) {
			try {
				return Integer.parseInt(currentWay.getMaxSpeed());
			} catch (NumberFormatException e) {
				Log.w(TAG, "Error parsing speed limit " + currentWay.getMaxSpeed());
			}
		}
		return null;
	}
	
	protected static class OSMListener implements OSMWayChangedListener {
		public void onOSMWayChangedListener(Way way) {
			Log.i(TAG, "Got OSM way " + way);
			currentWay = way;
			eventBus.post(new EventOSMWayChange(way));
			eventBus.post(new RadarMessageNotification("OSM way " + way + "\nspeed limit " + ( way != null ? way.getMaxSpeed() : "")));
		}
	}
	
	public static class EventOSMWayChange {
		public Way way;
		
		private EventOSMWayChange(Way w) {
			way = w;
		}
	}
}
