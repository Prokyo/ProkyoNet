package de.prokyo.network.common.packet;

import de.prokyo.network.common.buffer.PacketBuffer;

/**
 * Packet for keeping the connection alive.
 */
public class KeepAlivePacket implements Packet {

	@Override
	public void encode(PacketBuffer buffer) {
	}

	@Override
	public void decode(PacketBuffer buffer) {
	}

}
