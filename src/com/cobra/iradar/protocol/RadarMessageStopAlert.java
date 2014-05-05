package com.cobra.iradar.protocol;

public class RadarMessageStopAlert extends RadarMessage {
	
	private static final long serialVersionUID = 1L;
	
	public double batteryVoltage;
	
	public RadarMessageStopAlert(byte[] packet) throws Exception {
		super(packet);
		if ( type != RadarMessage.TYPE_STOP_ALERT ) {
			throw new Exception("Invalid packet for alert stop message");
		}
		if ( packet[5] == 49 ) 
			batteryVoltage = calcBatteryVoltage();
	}
	
	public RadarMessageStopAlert(double batteryVoltage)  {
		super(RadarMessage.TYPE_STOP_ALERT);
		this.batteryVoltage = batteryVoltage;
	}
	
	public double calcBatteryVoltage() {
		return (-48 + (100 * (-48 + packet[11]) + 10 * (-48 + packet[12]) + packet[13])) / 10.0D;
	}
	
}
