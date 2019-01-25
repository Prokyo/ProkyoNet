package de.prokyo.network.common.event;

import de.prokyo.network.common.connection.Connection;
import de.prokyo.network.common.packet.Packet;
import lombok.RequiredArgsConstructor;

/**
 * Event that will be called when an packet is read.
 */
@RequiredArgsConstructor
public class PacketIncomingEvent implements Event {

	private final Packet packet;
	private final Connection connection;

}
