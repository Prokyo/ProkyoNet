package de.prokyo.network.common.event;

/**
 * An EventHandler handles all triggered events of the type <i>T</i>.
 * @param <T> The type which this EventHandler shall listen to
 */
public interface EventHandler<T extends Event> {

	/**
	 * The method the {@link EventManager} calls when a fitting event was fired.
	 *
	 * @param event The fired event
	 */
	void handle(T event);

}
