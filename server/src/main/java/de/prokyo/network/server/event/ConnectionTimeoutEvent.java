package de.prokyo.network.server.event;

import de.prokyo.network.common.event.Event;
import de.prokyo.network.server.ClientConnection;
import lombok.Data;

/**
 * Event that will be called when a channel is inactive. It contains the {@link ClientConnection} the connection was established with previously.
 */
@Data
public class ConnectionTimeoutEvent implements Event {

	private final ClientConnection clientConnection;

}
