package com.cobra.iradar.messaging;

import com.cobra.iradar.protocol.RadarMessageAlert.Alert;

public class CobraMessageThreat extends CobraMessage {

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
	
	public CobraMessageThreat(Alert alert, int strength, float freq) {
		this.alertType = alert;
		this.strength = strength;
		this.frequency = freq;
	}
	
	/**
	 * Two threats are assumed equal if they are in same radar band and frequencies are within 0.2Ghz range of other
	 */
	@Override
	public boolean equals(Object t) {
		if ( t instanceof CobraMessageThreat ) {
			return ( (((CobraMessageThreat) t).alertType == this.alertType ) &&
					Math.abs(  ((CobraMessageThreat) t).frequency - this.frequency ) < 0.2f);
		}
		return false;
	}
	
	
}
