package de.prokyo.network.server;

import de.prokyo.network.common.event.OutgoingPacketEvent;
import de.prokyo.network.common.packet.Packet;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;

/**
 * Represents a packet handler for triggering events.
 */
@RequiredArgsConstructor
public class ProkyoDuplexHandler extends ChannelDuplexHandler {

	private final ProkyoServer prokyoServer;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(!(msg instanceof Packet)) return;

		this.prokyoServer.getEventManager().fire((Packet) msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if(!(msg instanceof Packet)) return;

		this.prokyoServer.getEventManager().fire(new OutgoingPacketEvent((Packet) msg));
	}

}
