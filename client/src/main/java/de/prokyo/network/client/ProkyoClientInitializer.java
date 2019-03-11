package de.prokyo.network.client;

import de.prokyo.network.common.event.KeepAliveEvent;
import de.prokyo.network.common.handler.ProkyoTimeOutHandler;
import de.prokyo.network.common.pipeline.PacketDecoder;
import de.prokyo.network.common.pipeline.PacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;

/**
 * Initializes the client connection.
 */
@RequiredArgsConstructor
public class ProkyoClientInitializer extends ChannelInitializer {

	private final ProkyoClient client;

	@Override
	protected void initChannel(Channel channel) throws Exception {
		channel.pipeline()
				.addLast(new IdleStateHandler(10, 10, 0))
				.addLast("frame-decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
				.addLast("prokyoDecoder", new PacketDecoder())
				.addLast("frame-prepender", new LengthFieldPrepender(4))
				.addLast("prokyoEncoder", new PacketEncoder())
				.addLast("prokyoTimeOutHandler", new ProkyoTimeOutHandler(this::triggerKeepAliveEvent))
				.addLast("prokyoPacketHandler", new ProkyoDuplexHandler(this.client));

		channel.attr(ProkyoClient.ATTRIBUTE_KEY).set(this.client);
	}

	private void triggerKeepAliveEvent(KeepAliveEvent event) {
		event.getUnsafe().setConnection(client);
		this.client.getEventManager().fire(event);
	}

}
