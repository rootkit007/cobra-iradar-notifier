package com.greatnowhere.radar.messaging;

import com.cobra.iradar.protocol.CobraRadarMessageAlert.Alert;

public class RadarMessageThreat extends RadarMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Alert alertType;
	
	/**
	 * value 0-5
	 */
	public Integer strength;
	
	public Float frequency;
	
	public RadarMessageThreat(Alert alert, int strength, float freq) {
		this.alertType = alert;
		this.strength = strength;
		this.frequency = freq;
	}
	
	/**
	 * Two threats are assumed equal if they are in same radar band and frequencies are within 0.2Ghz range of other
	 */
	@Override
	public boolean equals(Object t) {
		if ( t instanceof RadarMessageThreat ) {
			return ( (((RadarMessageThreat) t).alertType == this.alertType ) &&
					Math.abs(  ((RadarMessageThreat) t).frequency - this.frequency ) < 0.2f);
		}
		return false;
	}
	
	/**
	 * Cobra messages with freq=0 mean volume change
	 * @return
	 */
	public boolean isVolumeChangeMessage() {
		return ( frequency == null || frequency == 0f );
	}
	
	public int getVolume() {
		return strength;
	}
	
}
