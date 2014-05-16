package com.greatnowhere.radar.services;

import com.greatnowhere.radar.messaging.RadarMessageAllClear;
import com.greatnowhere.radar.messaging.RadarMessageConnectivityNotification;
import com.greatnowhere.radar.messaging.RadarMessageNotification;
import com.greatnowhere.radar.messaging.RadarMessageThreat;
import com.greatnowhere.radar.util.AbstractEventBusListener;

public abstract class CobraMessageHandler extends AbstractEventBusListener {
	
	public abstract void onEventBackgroundThread(final RadarMessageConnectivityNotification msg);
	public abstract void onEventBackgroundThread(final RadarMessageThreat msg);
	public abstract void onEventBackgroundThread(final RadarMessageAllClear msg);
	public abstract void onEventBackgroundThread(final RadarMessageNotification msg);
	
}
