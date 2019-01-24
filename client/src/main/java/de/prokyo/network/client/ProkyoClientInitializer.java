package de.prokyo.network.client;

import de.prokyo.network.common.pipeline.PacketDecoder;
import de.prokyo.network.common.pipeline.PacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * Initializes the client connection.
 */
public class ProkyoClientInitializer extends ChannelInitializer {

	@Override
	protected void initChannel(Channel channel) throws Exception {
		channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30))
				.addLast("frame-decoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4))
				.addLast("prokyoDecoder", new PacketDecoder())
				.addLast("frame-prepender", new LengthFieldPrepender(4))
				.addLast("prokyoEncoder", new PacketEncoder());

		channel.attr(ProkyoClient.ATTRIBUTE_KEY);
	}

}
