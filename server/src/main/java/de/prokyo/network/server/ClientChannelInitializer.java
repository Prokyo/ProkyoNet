package de.prokyo.network.server;

import de.prokyo.network.common.pipeline.PacketDecoder;
import de.prokyo.network.common.pipeline.PacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * Class for initializing socket channels of clients.
 */
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast("timeout", new ReadTimeoutHandler(30))
				.addLast("frame-decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
				.addLast("prokyoDecoder", new PacketDecoder())
				.addLast("frame-prepender", new LengthFieldPrepender(4))
				.addLast("prokyoEncoder", new PacketEncoder());

		ClientConnection connection = new ClientConnection(ch);
		ch.attr(ClientConnection.ATTRIBUTE_KEY).set(connection);
	}

}
