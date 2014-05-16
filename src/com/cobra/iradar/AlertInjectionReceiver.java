package com.cobra.iradar;

import java.util.Random;

import android.content.Context;
import android.content.Intent;

import com.cobra.iradar.protocol.CobraRadarMessageAlert;
import com.cobra.iradar.protocol.CobraRadarMessageAlert.Alert;
import com.greatnowhere.radar.receiver.BootReceiver;

import de.greenrobot.event.EventBus;

public class AlertInjectionReceiver extends BootReceiver {

	public static final String BROADCAST_INJECT_ALERT = "com.cobra.iradar.InjectRandomAlert";
	
	private static EventBus eventBus;
			
	@Override
	public void onReceive(Context context, Intent intent) {
		
		eventBus = EventBus.getDefault();
		Random r = new Random();
		Alert a = Alert.Ka;
		switch (r.nextInt(2)) {
		case 0:
			a = Alert.K;
			break;
		case 1:
			a = Alert.Ka;
			break;
		case 2:
			a = Alert.X;
			break;
		}
		CobraRadarMessageAlert msg = new CobraRadarMessageAlert(a, r.nextInt(4), Math.round(r.nextFloat()*150f + 200f)/10f, 3000L );
		eventBus.post(msg);
	}

}
