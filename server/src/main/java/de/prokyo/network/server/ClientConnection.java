package de.prokyo.network.server;

import de.prokyo.network.common.connection.Connection;
import de.prokyo.network.common.packet.Packet;
import de.prokyo.network.common.pipeline.ProkyoCompressor;
import de.prokyo.network.common.pipeline.ProkyoDecompressor;
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

	/**
	 * Add the {@link ProkyoCompressor} and the {@link ProkyoDecompressor} to the channel pipeline.
	 */
	public void enableCompression() {
		this.channel.pipeline()
				.addBefore("prokyoEncoder", "prokyoCompressor", new ProkyoCompressor())
				.addBefore("prokyoDecoder", "prokyoDecompressor", new ProkyoDecompressor());
	}

	/**
	 * Removes the {@link ProkyoCompressor} and the {@link ProkyoDecompressor} from the channel pipeline.
	 */
	public void disableCompression() {
		this.channel.pipeline().remove(ProkyoCompressor.class);
		this.channel.pipeline().remove(ProkyoDecompressor.class);
	}

	@Override
	public void sendPacket(Packet packet) {
		this.channel.writeAndFlush(packet);
	}

}
