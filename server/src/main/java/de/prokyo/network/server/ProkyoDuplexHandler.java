package de.prokyo.network.server;

import de.prokyo.network.common.event.OutgoingPacketEvent;
import de.prokyo.network.common.packet.Packet;
import de.prokyo.network.server.event.ConnectionClosedEvent;
import de.prokyo.network.server.event.ConnectionEstablishedEvent;
import de.prokyo.network.common.event.PacketIncomingEvent;
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
	private final ClientConnection connection;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.prokyoServer.getEventManager().fire(new ConnectionEstablishedEvent(this.connection));
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.prokyoServer.getEventManager().fire(new ConnectionClosedEvent(this.connection));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof Packet)) return;

		try {
			this.prokyoServer.getEventManager().fire((Packet) msg);
			this.prokyoServer.getEventManager().fire(new PacketIncomingEvent((Packet) msg, this.connection));
		} finally {
			super.channelRead(ctx, msg);
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (!(msg instanceof Packet)) return;

		try {
			this.prokyoServer.getEventManager().fire(new OutgoingPacketEvent((Packet) msg, this.connection));
		} finally {
			super.write(ctx, msg, promise);
		}
	}

}
