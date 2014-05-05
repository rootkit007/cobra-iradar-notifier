package com.cobra.iradar.protocol;

import java.io.InputStream;

public class RadarPacketProcessor {

	/**
	 * Reads a single iRadar packet from inputstream
	 * @param i
	 * @return
	 * @throws Exception 
	 */
	public static byte[] getPacket(InputStream i) throws Exception {
		
		byte[] buff = new byte[32];
		
		// All packets are 32 bytes long, start with 36 and end with 141
		while ( i.read() != 36 ) {};
		buff[0] = 36;
		i.read(buff, 1, 31);
		
		if ( buff[31] != (byte) 141 || calCheckSum(buff) != buff[30] ) {
			throw new Exception("Invalid packet received");
		}
		
		return buff;
		
	}
	
	/**
	 * Calculates radar packet checksum
	 * Excludes start and end bytes, as well as checksum byte
	 * @param paramArrayOfInt
	 * @return
	 */
	public static byte calCheckSum(byte[] paramArrayOfInt)
	{
		byte i = 0;
		for (byte j = 1; j<30; j++)
	    {
	      i ^= paramArrayOfInt[j];
	    }
		return i;
	}

	
}
