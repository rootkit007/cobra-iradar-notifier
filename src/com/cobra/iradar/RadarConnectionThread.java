package com.cobra.iradar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.cobra.iradar.messaging.ConnectivityStatus;
import com.cobra.iradar.protocol.RadarMessage;
import com.cobra.iradar.protocol.RadarMessageNotification;
import com.cobra.iradar.protocol.RadarMessageStopAlert;
import com.cobra.iradar.protocol.RadarPacketProcessor;

import de.greenrobot.event.EventBus;

public class RadarConnectionThread extends Thread {
	
	private static final String TAG = RadarConnectionThread.class.getCanonicalName(); 
	
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
	private BluetoothDevice iRadar;
	private BluetoothSocket socket;
	private InputStream rxStream;
	private OutputStream txStream;
    private EventBus eventBus;
    
    public static AtomicBoolean isRunning = new AtomicBoolean(false);
	
	public RadarConnectionThread(BluetoothDevice dev) {
		iRadar = dev;
		eventBus = EventBus.getDefault();
		setName("BT Connection");
	}
	
	@Override
	public synchronized void run() {
		
		isRunning.set(true);
		
		if ( iRadar == null ) {
			this.interrupt();
			isRunning.set(false);
			return;
		}
		
		boolean isConnectionSuccess = false;
		
		// connection attempt
		try {
			eventBus.post(new RadarMessageNotification(RadarMessageNotification.TYPE_CONN, "Connecting to " + iRadar.getName(),
					ConnectivityStatus.CONNECTING.getCode()));
			
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
			eventBus.post(new RadarMessageNotification("Connection failed"));
			isRunning.set(false);
			return;
		}

		eventBus.post(new RadarMessageNotification(RadarMessageNotification.TYPE_CONN, "Connected to iRadar device",
				ConnectivityStatus.CONNECTED.getCode()));
		isConnectionSuccess = true;

		byte[] packet;
		while ( !isInterrupted() ) {
			try {
				packet = RadarPacketProcessor.getPacket(rxStream);
				eventBus.post(RadarMessage.fromPacket(packet));
			} catch (Exception e) {
				Log.e(TAG, "IO Exception", e);
				eventBus.post(new RadarMessageStopAlert(0));
				eventBus.post(new RadarMessageNotification(RadarMessageNotification.TYPE_CONN, "Error in data connection",
						ConnectivityStatus.PROTOCOL_ERROR.getCode()));
				this.interrupt(); 
			}
		}
		
		try {
			Log.i(TAG, "Closing resources");
			rxStream.close();
			txStream.close();
			socket.close();
		} catch (IOException e) {
			Log.i(TAG, e.getLocalizedMessage());
		}
		
		// if we were successfully connected, notify clients of conn status change
		if ( isConnectionSuccess ) {
			eventBus.post(new RadarMessageStopAlert(0));
			eventBus.post(new RadarMessageNotification(RadarMessageNotification.TYPE_CONN, "Disconnected",
				ConnectivityStatus.DISCONNECTED.getCode()));
		}
		
		isRunning.set(false);
		
	}
	
	public synchronized void send(byte[] buf) throws IOException {
		if ( txStream != null ) {
			txStream.write(buf);
		}
	}
	
}
