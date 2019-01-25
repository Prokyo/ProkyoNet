package de.prokyo.network.common.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages all kind of stuff related to events.<br>
 * This class is completly thread safe, but blocking while (un-)registering handlers.
 */
public class EventManager {

	private final Map<Class<? extends Event>, List<EventHandler<? extends Event>>> eventClassToEventHandler
			= new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Registers the given event.
	 * This method is blocking the complete event manager while executing.
	 *
	 * @param clazz The class of the event
	 * @param eventHandler The event handler
	 * @param <T> The type of the event
	 */
	public <T extends Event> void register(Class<T> clazz, EventHandler<T> eventHandler) {
		lock.writeLock().lock();

		try {
			this.eventClassToEventHandler.computeIfAbsent(clazz, c -> new ArrayList<>()).add(eventHandler);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Unregisters the given event.
	 * This method is blocking the complete event manager while executing.
	 *
	 * @param eventHandler The event handler
	 */
	public void unregister(EventHandler eventHandler) {
		lock.writeLock().lock();

		try {
			for (Map.Entry<Class<? extends Event>, List<EventHandler<? extends Event>>> entry : this.eventClassToEventHandler.entrySet())
				if (entry.getValue().remove(eventHandler)) return;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Fires an event and calls all corresponding event handlers.
	 *
	 * @param event The event to fire
	 */
	public void fire(Event event) {
		List<EventHandler<? extends Event>> handlers = this.eventClassToEventHandler.get(event.getClass());
		if(handlers == null) return;
		for (int i = 0; i < handlers.size(); i++) {
			((EventHandler) handlers.get(i)).handle(event);
		}
	}

}
