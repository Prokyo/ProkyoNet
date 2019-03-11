package de.prokyo.network.client;

import de.prokyo.network.client.event.ConnectionClosedEvent;
import de.prokyo.network.client.event.ConnectionEstablishedEvent;
import de.prokyo.network.common.event.OutgoingPacketEvent;
import de.prokyo.network.common.event.PacketIncomingEvent;
import de.prokyo.network.common.packet.KeepAlivePacket;
import de.prokyo.network.common.packet.Packet;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;

/**
 * Represents a packet handler for triggering events.
 */
@RequiredArgsConstructor
public class ProkyoDuplexHandler extends ChannelDuplexHandler {

	private final ProkyoClient prokyoClient;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.prokyoClient.getEventManager().fire(new ConnectionEstablishedEvent(this.prokyoClient));
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.prokyoClient.getEventManager().fire(new ConnectionClosedEvent(this.prokyoClient));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof Packet)) return;

		System.out.println(msg.getClass().getSimpleName());

		try {
			this.prokyoClient.getEventManager().fire((Packet) msg);
			this.prokyoClient.getEventManager().fire(new PacketIncomingEvent((Packet) msg, this.prokyoClient));
		} finally {
			super.channelRead(ctx, msg);
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (!(msg instanceof Packet)) return;

		try {
			this.prokyoClient.getEventManager().fire(new OutgoingPacketEvent((Packet) msg, this.prokyoClient));
		} finally {
			super.write(ctx, msg, promise);
		}
	}
}
