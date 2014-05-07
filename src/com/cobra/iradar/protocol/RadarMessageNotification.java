package com.cobra.iradar.protocol;

import com.cobra.iradar.messaging.ConnectivityStatus;

/**
 * Notification message
 * @author pzeltins
 *
 */
public class RadarMessageNotification {
	
	public static final int TYPE_CONN = 0; // connection change
	public static final int TYPE_NOTIFY = 1; // message
	
	public int type;
	public String message;
	/**
	 * See @link {@link ConnectivityStatus} for values
	 */
	public int connectionStatus; 
	
	public RadarMessageNotification(int type, String message, int connStatus) {
		this.type = type;
		this.message = message;
		this.connectionStatus = connStatus;
	}
	
	/**
	 * Notification type message
	 * @param message
	 */
	public RadarMessageNotification(String message) {
		this(TYPE_NOTIFY, message, ConnectivityStatus.UNKNOWN.getCode());
	}


}
