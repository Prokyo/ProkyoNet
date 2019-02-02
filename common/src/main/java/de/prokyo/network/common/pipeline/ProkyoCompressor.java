package de.prokyo.network.common.pipeline;

import de.prokyo.network.common.buffer.PacketBuffer;
import de.prokyo.network.common.compression.CompressionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

/**
 * Represents a packet compressor.<br>
 * This implementation creates following footprint for every packet:<br>
 *     byte[] -> input.length - 4 (packet id)<br>
 *     byte[] -> compressed data (see {@link CompressionUtil#compress(byte[])}<br>
 *     PacketBuffer -> 4 bytes (packet id) + VarInt (rawData.length -> uncompressedSize) + compressedData.length (the data itself)<br>
 */
public class ProkyoCompressor extends MessageToMessageEncoder<ByteBuf> {

	private static final int COMPRESSION_THRESHOLD = 128;

	// #TODO: Should we compress the packet in {@link PacketEncoder} to generate a lower footprint?

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		PacketBuffer original = new PacketBuffer(msg);
		original.resetReaderIndex();
		int packetId = original.readInt();

		int packetBytes = original.readableBytes();
		byte[] rawData = new byte[packetBytes];
		original.readBytes(rawData);

		PacketBuffer buffer;
		byte informationByte = 0;
		if (packetBytes < COMPRESSION_THRESHOLD) {
			buffer = new PacketBuffer(5 + packetBytes);
			buffer.writeInt(packetId);
			buffer.writeByte(informationByte);
			buffer.writeBytes(rawData);
		} else {
			informationByte = 1;

			byte[] compressedData = CompressionUtil.getInstance().compress(rawData);

			buffer = new PacketBuffer(7 + compressedData.length);
			buffer.writeInt(packetId);
			buffer.writeByte(informationByte);
			buffer.writeVarInt(rawData.length);
			buffer.writeVarInt(compressedData.length);
			buffer.writeBytes(compressedData);
		}

		out.add(buffer);
	}

}
