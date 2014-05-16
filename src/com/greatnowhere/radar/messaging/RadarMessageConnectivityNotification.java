package com.greatnowhere.radar.messaging;

/**
 * Verbose message containing information about connectivity status
 * @author pzeltins
 *
 */
public class RadarMessageConnectivityNotification extends RadarMessageNotification {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public RadarMessageConnectivityNotification() {
	}
	
	public ConnectivityStatus status;
	
}
