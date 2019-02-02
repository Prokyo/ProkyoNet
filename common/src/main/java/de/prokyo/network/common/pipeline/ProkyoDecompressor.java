package de.prokyo.network.common.pipeline;

import de.prokyo.network.common.buffer.PacketBuffer;
import de.prokyo.network.common.compression.CompressionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

/**
 * Decompresses the incoming packets.
 * This implementation creates the following footprint:
 * 		compressedDataLength (compressedData)
 * 		uncompressedSize (uncompressedData)
 * 		4 (packetId) + uncompressedSize -> buffer
 */
public class ProkyoDecompressor extends MessageToMessageDecoder<ByteBuf> {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		PacketBuffer original = new PacketBuffer(msg);
		int packetId = original.readInt();
		byte informationByte = original.readByte();

		PacketBuffer buffer;
		if (informationByte == 0) {
			buffer = new PacketBuffer(4 + original.readableBytes());
			original.writerIndex(original.readerIndex());
			buffer.writeInt(packetId);
			buffer.writeBytes(original);
		} else {
			int uncompressedSize = original.readVarInt();
			int compressedDataLength = original.readVarInt();
			byte[] compressedData = new byte[compressedDataLength];
			original.readBytes(compressedData);

			byte[] uncompressedData = CompressionUtil.getInstance().decompress(compressedData, uncompressedSize);

			buffer = new PacketBuffer(4 + uncompressedSize);
			buffer.writeInt(packetId);
			buffer.writeBytes(uncompressedData);
		}

		out.add(buffer);
	}

}
