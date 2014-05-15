package com.greatnowhere.iradar.services;

import com.cobra.iradar.messaging.CobraMessageAllClear;
import com.cobra.iradar.messaging.CobraMessageConnectivityNotification;
import com.cobra.iradar.messaging.CobraMessageNotification;
import com.cobra.iradar.messaging.CobraMessageThreat;

import de.greenrobot.event.EventBus;

public abstract class CobraMessageHandler {
	
	private EventBus eventBus;
	
	public CobraMessageHandler() {
		eventBus = EventBus.getDefault();
		eventBus.register(this);
	}
	
	public void stop() {
		if ( eventBus.isRegistered(this))
			eventBus.unregister(this);
	}
	
	public abstract void onEventBackgroundThread(final CobraMessageConnectivityNotification msg);
	public abstract void onEventBackgroundThread(final CobraMessageThreat msg);
	public abstract void onEventBackgroundThread(final CobraMessageAllClear msg);
	public abstract void onEventBackgroundThread(final CobraMessageNotification msg);
	
}
