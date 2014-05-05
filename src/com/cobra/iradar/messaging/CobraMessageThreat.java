package com.cobra.iradar.messaging;

import com.cobra.iradar.protocol.RadarMessageAlert.Alert;

public class CobraMessageThreat extends CobraMessage {

	public Alert alertType;
	
	/**
	 * value 0-5
	 */
	public Integer strength;
	
	public Float frequency;
	
	public CobraMessageThreat() {
		
	}
	
	public CobraMessageThreat(Alert alert, int strength, float freq) {
		this.alertType = alert;
		this.strength = strength;
		this.frequency = freq;
	}
}
