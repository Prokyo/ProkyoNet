package de.prokyo.network.server.event;

import de.prokyo.network.common.event.Event;
import de.prokyo.network.server.ProkyoServer;
import lombok.Data;

/**
 * Event that will be called when the server started.
 */
@Data
public class ServerStartEvent implements Event {

	private final ProkyoServer prokyoServer;

}
