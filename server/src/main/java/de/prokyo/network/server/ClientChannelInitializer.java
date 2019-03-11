package de.prokyo.network.server;

import de.prokyo.network.common.event.KeepAliveEvent;
import de.prokyo.network.common.handler.ProkyoTimeOutHandler;
import de.prokyo.network.common.pipeline.PacketDecoder;
import de.prokyo.network.common.pipeline.PacketEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;

/**
 * Class for initializing socket channels of clients.
 */
@RequiredArgsConstructor
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final ProkyoServer prokyoServer;

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ClientConnection connection = new ClientConnection(ch);

		ch.pipeline()
				.addLast(new IdleStateHandler(10,  10, 0))
				.addLast("frame-decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
				.addLast("prokyoDecoder", new PacketDecoder())
				.addLast("frame-prepender", new LengthFieldPrepender(4))
				.addLast("prokyoEncoder", new PacketEncoder())
				.addLast("prokyoTimeOutHandler", new ProkyoTimeOutHandler(this::triggerKeepAliveEvent))
				.addLast("prokyoPacketHandler", new ProkyoDuplexHandler(this.prokyoServer, connection));
		ch.attr(ClientConnection.ATTRIBUTE_KEY).set(connection);
	}

	private void triggerKeepAliveEvent(KeepAliveEvent event) {
		ClientConnection connection = event.getUnsafe().getChannel().attr(ClientConnection.ATTRIBUTE_KEY).get();
		event.getUnsafe().setConnection(connection);
		this.prokyoServer.getEventManager().fire(event);
	}

}
