package com.greatnowhere.radar.location;

import java.util.concurrent.atomic.AtomicBoolean;

import org.wikispeedia.models.Marker;

import com.greatnowhere.osmclient.OSMLocationListener;
import com.greatnowhere.osmclient.OSMLocationListener.OSMWayChangedListener;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.location.PhoneActivityDetector.ActivityStatus;
import com.greatnowhere.radar.messaging.RadarMessageNotification;
import com.greatnowhere.wikispeedia.client.WikiSpeedChangeListener;
import com.greatnowhere.wikispeedia.client.WikiSpeedChangeListener.WikiSpeedChangedListener;
import com.xapi.models.Way;

import de.greenrobot.event.EventBus;
import android.content.Context;
import android.util.Log;

public class LocationInfoLookupManager {

	private static final String TAG = LocationInfoLookupManager.class.getCanonicalName();
	
	private static OSMLocationListener osmListener;
	private static WikiSpeedChangeListener wsListener;
	private static EventBus eventBus;
	private static Way currentWay;
	private static Context ctx;
	private static LocationInfoLookupManager instance;
	private static AtomicBoolean isRunning = new AtomicBoolean(false);
	
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
	private static boolean isActivated() {
		return ( Preferences.isLookupSpeedLimit() &&
				( Preferences.isLookupSpeedLimitOnlyInCarMode() ? PhoneActivityDetector.getIsCarMode() : true ) &&
				( Preferences.isLookupSpeedLimitOnlyWhenDriving() ? PhoneActivityDetector.getActivityStatus() == ActivityStatus.DRIVING : true ) );
	}
	
	/**
	 * Activates or stops location info lookup
	 */
	private synchronized static void activate() {
		if ( isRunning.get() && !isActivated() ) {
			stop();
		} else if ( !isRunning.get() && isActivated() ) {
			start();
		}
	}
	
	private static void start() {
		osmListener = new OSMLocationListener(ctx);
		osmListener.setOSMWayListener(new OSMListener());
		wsListener = new WikiSpeedChangeListener(ctx);
		wsListener.setWikiSpeedChangedListener(new WSListener());
		isRunning.set(true);
	}
	
	public static void stop() {
		if ( osmListener != null )
			osmListener.stop();
		osmListener = null;
		if ( wsListener != null )
			wsListener.stop();
		wsListener = null;
		isRunning.set(false);
	}

	public static String getCurrentWayName() {
		if ( currentWay != null )
			return currentWay.getRoadName();
		return null;
	}
	
	private static void setSpeedLimit(Double l) {
		eventBus.postSticky(new EventSpeedLimitChange(l));
	}
	
	public EventSpeedLimitChange getSpeedLimit() {
		return eventBus.getStickyEvent(EventSpeedLimitChange.class);
	}
	
	protected static class OSMListener implements OSMWayChangedListener {
		public void onOSMWayChangedListener(Way way) {
			Log.i(TAG, "Got OSM way " + way);
			currentWay = way;
			if ( currentWay != null && currentWay.getMaxSpeed() != null ) {
					setSpeedLimit(currentWay.getMaxSpeed());
			}
			eventBus.post(new EventOSMWayChange(way));
			eventBus.post(new RadarMessageNotification("OSM way " + way + "\nspeed limit " + ( way != null ? way.getMaxSpeed() : "")));
		}
	}
	
	protected static class WSListener implements WikiSpeedChangedListener {
		public void onWikiSpeedChangedListener(Marker m) {
			Log.i(TAG, "Got WS marker " + m);
			if ( m != null ) {
				setSpeedLimit(m.getSpeedLimitMS());
			}
		}
	}
	
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
		
		public EventSpeedLimitChange(Double l) {
			limit = l;
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
	public void onEvent(PhoneActivityDetector.EventActivityChanged event) {
		activate();
	}

	public void onEvent(PhoneActivityDetector.EventCarModeChange event) {
		activate();
	}
	
	public void onEvent(Preferences.PreferenceLocationLookupSettingsChangedEvent event) {
		activate();
	}
	
}
