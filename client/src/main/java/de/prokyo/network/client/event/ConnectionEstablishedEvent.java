package de.prokyo.network.client.event;

import de.prokyo.network.client.ProkyoClient;
import de.prokyo.network.common.event.Event;
import lombok.Data;

/**
 * Event that will be called when a channel is activated.
 */
@Data
public class ConnectionEstablishedEvent implements Event {

	private final ProkyoClient prokyoClient;

}
