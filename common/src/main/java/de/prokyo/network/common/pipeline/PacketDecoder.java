package de.prokyo.network.common.pipeline;

import de.prokyo.network.common.buffer.PacketBuffer;
import de.prokyo.network.common.exception.DecodingException;
import de.prokyo.network.common.packet.Packet;
import de.prokyo.network.common.packet.PacketRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

/**
 * Decodes the encoded data and creates a new instance of the packet containing the information for the next handlers.
 */
public class PacketDecoder extends ByteToMessageDecoder {


	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		PacketBuffer buffer = new PacketBuffer(in);

		int packetId = buffer.readInt();
		if(packetId < 0) throw new DecodingException("A packet id can't be lower than zero.");

		Packet packet = PacketRegistry.getInstance().newInstance(packetId);
		if(packet == null) throw new DecodingException("Unknown packet id: " + packetId);

		packet.decode(buffer);

		out.add(packet);
	}

}
