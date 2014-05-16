package com.greatnowhere.radar.messaging;

/**
 * Verbose text message sent to UI
 * @author pzeltins
 *
 */
public class RadarMessageNotification extends RadarMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String message;
	
	public RadarMessageNotification() {
		
	}

	public RadarMessageNotification(String s) {
		this.message = s;
	}

}
