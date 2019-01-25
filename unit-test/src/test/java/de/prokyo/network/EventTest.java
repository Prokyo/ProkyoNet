package de.prokyo.network;

import de.prokyo.network.common.event.Event;
import de.prokyo.network.common.event.EventHandler;
import de.prokyo.network.common.event.EventManager;
import org.junit.Assert;
import org.junit.Test;

public class EventTest {

	private boolean eventFired;

	@Test
	public void testEvent() {
		EventManager eventManager = new EventManager();

		EventHandler<TestEvent> eventHandler = this::handleTestEvent;

		// registering
		eventManager.register(TestEvent.class, eventHandler);
		eventManager.fire(new TestEvent());
		Assert.assertTrue(this.eventFired);

		// unregistering
		this.eventFired = false;
		eventManager.unregister(eventHandler);
		eventManager.fire(new TestEvent());
		Assert.assertFalse(this.eventFired);
	}

	public void handleTestEvent(TestEvent event) {
		this.eventFired = true;
	}

	public static class TestEvent implements Event {
	}

}
