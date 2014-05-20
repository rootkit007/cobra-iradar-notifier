package com.greatnowhere.radar.util;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Notification.Builder;
import android.content.Context;
import android.content.Intent;

import com.cobra.iradar.RadarManager;
import com.cobra.iradar.RadarScanManager;
import com.greatnowhere.radar.MainRadarActivity;
import com.greatnowhere.radar.R;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.config.Preferences.PreferenceOngoingNotificationsChangedEvent;

/**
 * Builds and stores ongoing notifications
 * @author pzeltins
 *
 */
public class NotificationBuilder extends AbstractEventBusListener {
	
	private static Notification scanNotification = null;
	private static Notification connectedNotification = null;
	
	@SuppressWarnings("unused")
	private static NotificationBuilder instance;
	
	public static void init(Context ctx) {
	    // ongoing notifications
    	Builder b;
    	Intent resumeAppIntent;
		b = new Notification.Builder(ctx);
		b.setContentText("iRadar Scanning For Devices");
		resumeAppIntent = new Intent(ctx, MainRadarActivity.class);
		resumeAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
		b.setContentIntent(PendingIntent.getActivity(ctx, 0, resumeAppIntent , 0));
		b.setContentTitle("iRadar Notifier");
		b.setOngoing(true);
		b.setSmallIcon(R.drawable.app_icon);
		scanNotification = b.build();

		b = new Notification.Builder(ctx);
		b.setContentText("iRadar Notifier Connected");
		resumeAppIntent = new Intent(ctx, MainRadarActivity.class);
		resumeAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
		b.setContentIntent(PendingIntent.getActivity(ctx, 0, resumeAppIntent , 0));
		b.setContentTitle("iRadar Notifier");
		b.setOngoing(true);
		b.setSmallIcon(R.drawable.app_icon);
		connectedNotification = b.build();

		instance = new NotificationBuilder();
	}
	
	/**
	 * Returns "scanning" notification 
	 * If preferences specify no notification, returns null
	 * @return
	 */
	public static Notification getScanNotification() {
    	if ( Preferences.isNotifyOngoingScan() ) {
    		return scanNotification;
    	}
    	return null;
		
	}
    	
   	public static Notification getConnectedNotification() {
    	if ( Preferences.isNotifyOngoingConnected() ) {
    		return connectedNotification;
    	}
    	return null;
   	}

   	
   	public void onEventBackgroundThread(PreferenceOngoingNotificationsChangedEvent event) {
   		RadarScanManager.setNotification(getScanNotification());
   		RadarManager.setOngoingNotification(getConnectedNotification());
   	}
}
