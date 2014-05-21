package com.greatnowhere.radar.util;

import de.greenrobot.event.EventBus;

/**
 * Base class for anyone using eventbus
 * @author pzeltins
 *
 */
public abstract class AbstractEventBusListener {
	
	private EventBus eventBus;
	
	public AbstractEventBusListener() {
		eventBus = EventBus.getDefault();
		eventBus.register(this);
	}
	
	public void unRegister() {
		if ( eventBus.isRegistered(this))
			eventBus.unregister(this);
	}

}
