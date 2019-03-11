package de.prokyo.network.common.handler;

import de.prokyo.network.common.event.KeepAliveEvent;
import de.prokyo.network.common.packet.KeepAlivePacket;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ProkyoTimeOutHandler extends MessageToMessageDecoder<KeepAlivePacket> {

	private final Consumer<KeepAliveEvent> eventListener;

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
		if(!(evt instanceof IdleStateEvent)) return;
		ctx.channel().writeAndFlush(new KeepAlivePacket(System.currentTimeMillis(), false));
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, KeepAlivePacket msg, List<Object> out) throws Exception {
		KeepAlivePacket packet = (KeepAlivePacket) msg;
		if(packet.isForwarded()) {
			this.eventListener.accept(new KeepAliveEvent(packet, ctx.channel()));
			return;
		}

		packet.setForwarded(true);
		ctx.channel().writeAndFlush(packet);
	}

}
