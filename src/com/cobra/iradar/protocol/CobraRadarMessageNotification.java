package com.cobra.iradar.protocol;

import com.greatnowhere.radar.messaging.ConnectivityStatus;

/**
 * Notification message
 * @author pzeltins
 *
 */
public class CobraRadarMessageNotification {
	
	public static final int TYPE_CONN = 0; // connection change
	public static final int TYPE_NOTIFY = 1; // message
	
	public int type;
	public String message;
	/**
	 * See @link {@link ConnectivityStatus} for values
	 */
	public int connectionStatus; 
	
	public CobraRadarMessageNotification(int type, String message, int connStatus) {
		this.type = type;
		this.message = message;
		this.connectionStatus = connStatus;
	}
	
	/**
	 * Notification type message
	 * @param message
	 */
	public CobraRadarMessageNotification(String message) {
		this(TYPE_NOTIFY, message, ConnectivityStatus.UNKNOWN.getCode());
	}


}
