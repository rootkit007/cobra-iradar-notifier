package com.cobra.iradar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class RadarConnectionThread extends Thread {
	
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
	private BluetoothDevice iRadar;
	private Handler handler;
	private BluetoothSocket socket;
	private InputStream rxStream;
	private OutputStream txStream;
	
	public RadarConnectionThread(BluetoothDevice dev, Handler handler) {
		iRadar = dev;
		this.handler = handler; 
	}
	
	@Override
	public synchronized void run() {
		
		if ( iRadar == null || handler == null ) {
			this.interrupt();
			return;
		}
		
		// connection attempt
		try {
			try {
				socket = iRadar.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (Exception ex) {
				socket = iRadar.createInsecureRfcommSocketToServiceRecord(MY_UUID);
			}
			
			BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
			socket.connect();
			rxStream = socket.getInputStream();
			txStream = socket.getOutputStream();
			
		} catch (Exception e) {
			Message m = handler.obtainMessage(RadarConnectionService.MSG_IRADAR_FAILURE, e.getLocalizedMessage());
			handler.handleMessage(m);
		}
		
		byte[] packet;
		while ( !isInterrupted() ) {
			try {
				packet = RadarPacketProcessor.getPacket(rxStream);
				Message m = handler.obtainMessage(RadarConnectionService.MSG_RADAR_MESSAGE_RECV);
				Bundle b = new Bundle();
				b.putSerializable(RadarConnectionService.BUNDLE_KEY_RADAR_MESSAGE, RadarMessage.fromPacket(packet));
				m.setData(b);
				handler.handleMessage(m);
			} catch (Exception e) {
				Message m = handler.obtainMessage(RadarConnectionService.MSG_IRADAR_FAILURE, e.getLocalizedMessage());
				handler.handleMessage(m);
				this.interrupt();
			}
			
		}
		
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	public synchronized void send(byte[] buf) throws IOException {
		if ( txStream != null ) {
			txStream.write(buf);
		}
	}

}
