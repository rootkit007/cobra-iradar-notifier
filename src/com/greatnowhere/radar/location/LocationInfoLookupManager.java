package com.greatnowhere.radar.location;

import java.util.concurrent.atomic.AtomicBoolean;

//import org.wikispeedia.models.Marker;

import android.content.Context;
import android.util.Log;

import com.cobra.iradar.CobraRadarEvents;
import com.cobra.iradar.RadarManager;
import com.greatnowhere.osmclient.OSMLocationListener;
import com.greatnowhere.osmclient.OSMLocationListener.OSMWayChangedListener;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.messaging.RadarMessageNotification;
//import com.greatnowhere.wikispeedia.client.WikiSpeedChangeListener;
//import com.greatnowhere.wikispeedia.client.WikiSpeedChangeListener.WikiSpeedChangedListener;
import com.xapi.models.Way;

import de.greenrobot.event.EventBus;

public class LocationInfoLookupManager {

	private static final String TAG = LocationInfoLookupManager.class.getCanonicalName();
	
	private static OSMLocationListener osmListener;
	//private static WikiSpeedChangeListener wsListener;
	private static EventBus eventBus;
	private static Way currentWay;
	private static Context ctx;
	private static LocationInfoLookupManager instance;
	private static AtomicBoolean isRunning = new AtomicBoolean(false);
	
	public static final String SOURCE_WS = "WikiSpeedia";
	public static final String SOURCE_OSM = "OpenStreetMaps";
	
	public static void init(Context ctx) {
		LocationInfoLookupManager.ctx = ctx;
		instance = new LocationInfoLookupManager();
		eventBus = EventBus.getDefault();
		eventBus.register(instance);
		activate();
	}
	
	/**
	 * Returns TRUE if location info lookup should be active as per preferences
	 * @return
	 */
	public static boolean isShouldActivate() {
		return ( Preferences.isLookupSpeedLimit() && RadarManager.isRadarConnected()
//				( Preferences.isLookupSpeedLimitOnlyInCarMode() ? PhoneActivityDetector.getIsCarMode() : true ) &&
//				( Preferences.isLookupSpeedLimitOnlyWhenDriving() ? PhoneActivityDetector.isActivityDriving() : true ) &&
//				( Preferences.isLookupSpeedLimitOnlyWhenRadarConnected() ? RadarManager.isRadarConnected() : true ) 
				);
	}
	
	/**
	 * Activates or stops location info lookup
	 */
	private synchronized static void activate() {
		Log.d(TAG,"activate()");
		if ( isRunning.get() && !isShouldActivate() ) {
			stop();
		} else if ( !isRunning.get() && isShouldActivate() ) {
			start();
		}
	}
	
	private static void start() {
		Log.i(TAG, "Starting OSM and WS clients");
		try {
			osmListener = new OSMLocationListener(ctx);
			osmListener.setMaxRequests(1);
			osmListener.setOSMWayListener(new OSMListener());
		} catch (Exception ex) {
			Log.w(TAG, ex);
		}
		try {
			//wsListener = new WikiSpeedChangeListener(ctx, Preferences.getWikiSpeediaUserName());
			//wsListener.setWikiSpeedChangedListener(new WSListener());
		} catch (Exception ex) {
			Log.w(TAG, ex);
		}
		isRunning.set(true);
	}
	
	public static void stop() {
		Log.i(TAG,"Stopping OSM and WS clients");
		if ( osmListener != null )
			osmListener.stop();
		osmListener = null;
		//if ( wsListener != null )
			//wsListener.stop();
		//wsListener = null;
		isRunning.set(false);
	}
	
	public static void destroy() {
		Log.i(TAG,"destroy");
		stop();
		if ( eventBus != null && instance != null && eventBus.isRegistered(instance) )
			eventBus.unregister(instance);
	}

	public static String getCurrentWayName() {
		if ( currentWay != null )
			return currentWay.getRoadName();
		return null;
	}
	
	private static void setSpeedLimit(Double l, String source) {
		eventBus.postSticky(new EventSpeedLimitChange(l,source));
	}
	
	public EventSpeedLimitChange getSpeedLimit() {
		return eventBus.getStickyEvent(EventSpeedLimitChange.class);
	}
	
	protected static class OSMListener implements OSMWayChangedListener {
		public void onOSMWayChangedListener(Way way) {
			Log.i(TAG, "Got OSM way " + ( way == null ? "null" : way.toString()));
			currentWay = way;
			if ( currentWay != null ) {
				setSpeedLimit(currentWay.getMaxSpeed(), SOURCE_OSM);
			}
			eventBus.post(new EventOSMWayChange(way));
			if ( way == null ) {
				eventBus.post(new RadarMessageNotification("OSM missing data"));
			} else {
				eventBus.post(new RadarMessageNotification("OSM way " + way + "\nspeed limit " + ( way != null ? way.getMaxSpeed() : "")));
			}
		}
	}
	
	/*
	protected static class WSListener implements WikiSpeedChangedListener {
		public void onWikiSpeedChangedListener(Marker m) {
			Log.i(TAG, "Got WS marker " + m);
			if ( m != null ) {
				setSpeedLimit(m.getSpeedLimitMS(), SOURCE_WS);
			}
		}
	}
	*/
	
	public static class EventOSMWayChange {
		public Way way;
		
		private EventOSMWayChange(Way w) {
			way = w;
		}
	}
	
	public static class EventSpeedLimitChange {
		
		public static final double KPH_TO_MS = 0.277778D;
		public static final double MPH_TO_MS = 0.44704D;
		public static final double KNOTS_TO_MS = 0.514444D;
		
		public Double limit;
		
		public String source;
		
		/**
		 * 
		 * @param l speed limit in m/s
		 * @param s source
		 */
		public EventSpeedLimitChange(Double l,String s) {
			limit = l;
			source = s;
		}
		
		public Integer getKPH() {
			return Double.valueOf((Math.floor(limit / KPH_TO_MS))).intValue();
		}

		public Integer getMPH() {
			return Double.valueOf((Math.floor(limit / MPH_TO_MS))).intValue();
		}
	}

	/**
	 * Event handlers to start/stop lookup
	 * @param event
	 */
	public void onEventMainThread(PhoneActivityDetector.EventActivityChanged event) {
		activate();
	}

	public void onEventMainThread(PhoneActivityDetector.EventCarModeChange event) {
		activate();
	}
	
	public void onEventMainThread(Preferences.PreferenceLocationLookupSettingsChangedEvent event) {
		activate();
	}
	
	public void onEventMainThread(CobraRadarEvents.EventDeviceConnected event) {
		activate();
	}

	public void onEventMainThread(CobraRadarEvents.EventDeviceDisconnected event) {
		activate();
	}
}
