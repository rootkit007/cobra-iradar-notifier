package com.cobra.iradar;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * iRadar manager class. Starts background connectivity service as required 
 * @author pzeltins
 *
 */
public class IRadarManager {

	  public static String TAG = "IRadarManager";
	  
	  private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	  private static BluetoothDevice mBTDevice = null;
	  private static Context appContext;
	  private static IBinder radarServiceBinder;
	  private static Messenger radarServiceMessenger;
	  private static Handler radarMessageHandler;
	  private static PendingIntent reconnectionIntent;
	  
	  private static AtomicBoolean serviceBound = new AtomicBoolean(false);
	  
	  private static ServiceConnection radarConn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			sendNotificationMessage("iRadar service stopped!");
            radarServiceBinder = null;
            radarServiceMessenger = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "ON Service Connected");
			sendNotificationMessage("iRadar service started!");
            radarServiceBinder = service;
            radarServiceMessenger = new Messenger(radarServiceBinder);
            Message m = Message.obtain();
            m.replyTo = new Messenger(radarMessageHandler);
            m.what = RadarConnectionService.MSG_REGISTER_LISTENER;
            try {
				radarServiceMessenger.send(m);
			} catch (RemoteException e) {
				sendNotificationMessage("Error connecting to iRadar service!");
			}
            // Set BT device
            m = Message.obtain();
            m.what = RadarConnectionService.MSG_SET_IRADAR_DEVICE;
            m.obj = mBTDevice;
            try {
				radarServiceMessenger.send(m);
			} catch (RemoteException e) {
				sendNotificationMessage("Error connecting to iRadar service!");
			}
		}
	  };
	  
	  public static synchronized void initialize(Context ctx, Handler parmRadarMessageHandler) {

		  	appContext = ctx;
		  	radarMessageHandler = parmRadarMessageHandler;
		  
	        // If the adapter is null, then Bluetooth is not supported
	        if (mBluetoothAdapter == null) {
	            sendNotificationMessage("Bluetooth is not available");
	            return;
	        }
	        
	        for ( BluetoothDevice dev : mBluetoothAdapter.getBondedDevices() ) {
	        	if ( Constants.BT_DEVICE_NAMES.contains(dev.getName()) ) {
	        		sendNotificationMessage("Connecting to " + dev.getName());
	        		connectRadarDevice(dev);
	        		return;
	        	}
	        }
	        sendNotificationMessage("iRadar device not paired!");
            
	  }
	  
	  public static synchronized void stop() {
		  Message stopMessage = Message.obtain();
		  stopMessage.what = RadarConnectionService.MSG_STOP;
		  try {
			  radarServiceMessenger.send(stopMessage);
		  } catch (RemoteException ex) {
			  ex.printStackTrace();
		  }
		  if ( serviceBound.get() ) {
			  appContext.unbindService(radarConn);
			  serviceBound.set(false);
		  }
		  stopConnectionMonitor();
	  }
	  
	  public static synchronized void tryReconnect() {
		  if ( radarServiceMessenger != null ) {
			  Message msg = Message.obtain();
			  msg.what = RadarConnectionService.MSG_RECONNECT;
			  try {
				  radarServiceMessenger.send(msg);
			  } catch (RemoteException ex) {
				  ex.printStackTrace();
			  }
		  }
	  }
	  
	  /**
	   * Starts periodic attempts to monitor/reconnect
	   * @param seconds
	   */
	  public static void startConnectionMonitor(int seconds) {
		  AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		  Intent reconnectIntent = new Intent(appContext, RadarMonitorService.class);
		  reconnectIntent.putExtra(RadarMonitorService.INTENT_RECONNECT, true);
		  reconnectionIntent = PendingIntent.getService(appContext, 0, reconnectIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		  am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000L, ((long) seconds) * 1000L, 
				  reconnectionIntent);
	  }
	  
	  public static void stopConnectionMonitor() {
		  if ( reconnectionIntent != null ) {
			  AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
			  am.cancel(reconnectionIntent);
			  reconnectionIntent = null;
		  }
	  }
	  
	  private static synchronized void connectRadarDevice( BluetoothDevice dev ) {
		  mBTDevice = dev;
		  if ( !serviceBound.get() ) {
			  appContext.bindService(new Intent(appContext, RadarConnectionService.class), radarConn, Context.BIND_AUTO_CREATE );
			  serviceBound.set(true);
		  }
	  }
	  
	  private static void sendNotificationMessage(String msg) {
		  if ( radarMessageHandler != null ) {
			  Message m = radarMessageHandler.obtainMessage(RadarConnectionService.MSG_NOTIFICATION, msg);
			  radarMessageHandler.handleMessage(m);
		  }
	  }
	  
 
}
