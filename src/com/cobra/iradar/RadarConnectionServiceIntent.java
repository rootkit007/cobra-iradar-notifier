package com.cobra.iradar;

import android.app.Notification;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

public class RadarConnectionServiceIntent extends Intent {

	public static final String RADAR_DEVICE = "iRadarDevice";
	public static final String RADAR_NOTIFICATION = "iRadarNotify";
	
	/**
	 * 
	 * @param ctx
	 * @param radarDevice - BT device to connect to
	 * @param fgNotify - if not null, will run ongoing notification tied to the service
	 */
	public RadarConnectionServiceIntent(Context ctx, BluetoothDevice radarDevice, Notification fgNotify) {
		super(ctx, RadarConnectionService.class);
		this.putExtra(RADAR_DEVICE, radarDevice);
		this.putExtra(RADAR_NOTIFICATION, fgNotify);
	}
	
	public BluetoothDevice getRadarDevice() {
		return (BluetoothDevice) getExtras().get(RADAR_DEVICE);
	}
	
	public Notification getNotification() {
		return (Notification) getExtras().get(RADAR_NOTIFICATION);
	}
	
}
