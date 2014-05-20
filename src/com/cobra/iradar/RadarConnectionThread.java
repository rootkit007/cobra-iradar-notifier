package com.cobra.iradar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.cobra.iradar.protocol.CobraRadarMessage;
import com.cobra.iradar.protocol.CobraRadarMessageNotification;
import com.cobra.iradar.protocol.CobraRadarMessageStopAlert;
import com.cobra.iradar.protocol.CobraRadarPacketProcessor;
import com.greatnowhere.radar.messaging.ConnectivityStatus;

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
    private static AtomicInteger connectionStatus = new AtomicInteger(ConnectivityStatus.UNKNOWN.getCode());
	
	public RadarConnectionThread(BluetoothDevice dev) {
		iRadar = dev;
		eventBus = EventBus.getDefault();
		setName("BT Connection " + getId());
	}
	
	protected static ConnectivityStatus getConnectivityStatus() {
		return ConnectivityStatus.fromCode(connectionStatus.get());
	}
	
	@Override
	public synchronized void run() {
		
		Log.i(TAG,"BT thread " + this.getId() + " starting");
		
		isRunning.set(true);
		
		if ( iRadar == null ) {
			this.interrupt();
			isRunning.set(false);
			connectionStatus.set(ConnectivityStatus.UNKNOWN.getCode());
			return;
		}
		
		boolean isConnectionSuccess = false;
		
		// connection attempt
		try {
			connectionStatus.set(ConnectivityStatus.CONNECTING.getCode());
			eventBus.post(new CobraRadarMessageNotification(CobraRadarMessageNotification.TYPE_CONN, "Connecting to " + iRadar.getName(),
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
			connectionStatus.set(ConnectivityStatus.DISCONNECTED.getCode());
			eventBus.post(new CobraRadarMessageNotification("Connection failed"));
			isRunning.set(false);
			return;
		}

		connectionStatus.set(ConnectivityStatus.CONNECTED.getCode());
		eventBus.post(new CobraRadarMessageNotification(CobraRadarMessageNotification.TYPE_CONN, "Connected to iRadar device",
				ConnectivityStatus.CONNECTED.getCode()));
		isConnectionSuccess = true;

		byte[] packet;
		while ( !isInterrupted() ) {
			try {
				packet = CobraRadarPacketProcessor.getPacket(rxStream);
				eventBus.post(CobraRadarMessage.fromPacket(packet));
			} catch (Exception e) {
				Log.e(TAG, "IO Exception", e);
				connectionStatus.set(ConnectivityStatus.PROTOCOL_ERROR.getCode());
				eventBus.post(new CobraRadarMessageStopAlert(0));
				eventBus.post(new CobraRadarMessageNotification(CobraRadarMessageNotification.TYPE_CONN, "Error in data connection",
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
		
		connectionStatus.set(ConnectivityStatus.DISCONNECTED.getCode());
		// if we were successfully connected, notify clients of conn status change
		if ( isConnectionSuccess ) {
			eventBus.post(new CobraRadarMessageStopAlert(0));
			eventBus.post(new CobraRadarMessageNotification(CobraRadarMessageNotification.TYPE_CONN, "Disconnected",
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
