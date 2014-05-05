package com.cobra.iradar.protocol;

import java.io.Serializable;

/**
 * Base class for radar messages
 * 
 * rxBuffer[4] == 65: alert
 *    and if (rxBuffer[5] == 118) : alert volume changed (automute?), value in rxBuffer[6]
 *    and if rxBuffer[5] == 88) || (rxBuffer[5] == 75) || (rxBuffer[5] == 65) || (rxBuffer[5] == 107) || (rxBuffer[5] == 85) || (rxBuffer[5] == 80) || (rxBuffer[5] == 86) || (rxBuffer[5] == 98
 *    	: threat of some kind
 *       strength in rxBuffer[6]
 *       frequency in l2 = 256 * (256 * rxBuffer[8]) + 256 * rxBuffer[9] + rxBuffer[10];
            f = (float)l2 / 1000.0F;
 * rxBuffer[4] == 78 : finish radar alert 
 *    and if rxBuffer[5] == 49: battery voltage in 11,12,13
 *           return (-48 + (100 * (-48 + paramArrayOfInt[11]) + 10 * (-48 + paramArrayOfInt[12]) + paramArrayOfInt[13])) / 10.0D;

 * if (rxBuffer[4] == 82) {
         and (rxBuffer[5] == 73) : detector type? fwnum in buffer[22:29]
 * rxBuffer[2] != 0
 * rxBuffer[5] == 83 : settings
 * rxBuffer[5] == 77 : mute alert received
 * rxBuffer[4] == 67 && rxBuffer[5] == 85 && rxBuffer[6] == 111 : display message
          
 * @author pzeltins
 *
 */
public class RadarMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final byte TYPE_ALERT = 65;
	public static final byte TYPE_STOP_ALERT = 78;
	public static final byte TYPE_DISPLAY = 67;
	public static final byte TYPE_SETTINGS = 82;
	
	protected byte[] packet;
	
	/**
	 * Message type
	 */
	public int type;
	
	public RadarMessage(byte[] packet) {
		type = packet[4];
		this.packet = packet;
	}
	
	public RadarMessage(byte type) {
		this.type = type;
		this.packet = new byte[32];
		this.packet[4] = type;
	}
	
	public static RadarMessage fromPacket(byte[] packet) throws Exception {
		switch (packet[4]) {
		case TYPE_ALERT:
			return new RadarMessageAlert(packet);
		case TYPE_STOP_ALERT:
			return new RadarMessageStopAlert(packet);
		default:
			return new RadarMessage(packet);
		}
	}
	
}
