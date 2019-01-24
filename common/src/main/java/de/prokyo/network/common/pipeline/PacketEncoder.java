package de.prokyo.network.common.pipeline;

import de.prokyo.network.common.buffer.PacketBuffer;
import de.prokyo.network.common.exception.EncodingException;
import de.prokyo.network.common.packet.Packet;
import de.prokyo.network.common.packet.PacketRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encodes packets to bytes and writes them to the output buffers.
 */
public class PacketEncoder extends MessageToByteEncoder<Packet> {

	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf) throws Exception {
		PacketBuffer buffer = new PacketBuffer(byteBuf);

		int packetId = PacketRegistry.getInstance().getPacketId(packet.getClass());
		if(packetId == -404) throw new EncodingException("The class " + packet.getClass() + " is not registered as a packet.");
		if(packetId < 0 && !PacketRegistry.getInstance().isReservedPacket(packetId))
			throw new EncodingException("A packet id can't be lower than zero.");

		buffer.writeInt(packetId);
		packet.encode(buffer);
	}

}
