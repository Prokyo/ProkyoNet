package de.prokyo.network.server;

import de.prokyo.network.common.connection.Connection;
import de.prokyo.network.common.packet.Packet;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;

/**
 * Class representing a channel with a client.
 */
@RequiredArgsConstructor
public class ClientConnection implements Connection {

	public static final AttributeKey<ClientConnection> ATTRIBUTE_KEY = AttributeKey.newInstance("PROKYO_CLIENT");

	private final Channel channel;

	@Override
	public void sendPacket(Packet packet) {
		this.channel.writeAndFlush(packet);
	}

}
