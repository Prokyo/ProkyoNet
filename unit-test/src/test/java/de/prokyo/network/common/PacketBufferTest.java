package de.prokyo.network.common;

import de.prokyo.network.common.buffer.PacketBuffer;
import java.nio.charset.Charset;
import de.prokyo.network.common.compression.CompressionUtil;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the packet buffer.
 */
public class PacketBufferTest {

	@Test
	public void testByteArrayOperation() {
		final byte[] data = "This is a test string".getBytes();
		PacketBuffer buffer = new PacketBuffer();

		buffer.writeByteArray(data);
		assertArrayEquals(data, buffer.readByteArray());
	}

	/**
	 * Tests compressing and decompressing data.
	 */
	@Test
	public void testCompression() {
		PacketBuffer buffer = new PacketBuffer();
		final byte[] data = new byte[512];
		for(int i = 0; i < data.length; i++) data[i] = (byte) i;

		buffer.compressAndWriteByteArray(data);
		byte[] uncompressedData = buffer.readAndDecompress();

		assertArrayEquals(data, uncompressedData);
		assertTrue(buffer.capacity() < data.length);
	}

	/**
	 * Tests the constructors of PacketBuffer.
	 */
	@Test
	public void testConstructors() {
		PacketBuffer buffer = new PacketBuffer();
		assertEquals(4, buffer.capacity());

		buffer = new PacketBuffer(11);
		assertEquals(11, buffer.capacity());

		buffer = new PacketBuffer(Unpooled.buffer(5));
		assertEquals(5, buffer.capacity());
	}

	/**
	 * Tests the writing and reading of a varint.
	 */
	@Test
	public void testVarInt() {
		int varint = 23;
		PacketBuffer buffer = new PacketBuffer(1);

		buffer.writeVarInt(varint);

		Assert.assertEquals(1, buffer.readableBytes());

		buffer.resetReaderIndex();

		Assert.assertEquals(varint, buffer.readVarInt());
	}

	/**
	 * Tests the writing and reading of an unsigned varint.
	 */
	@Test
	public void testUnsignedVarInt() {
		long varint = 0x88888888;
		PacketBuffer buffer = new PacketBuffer(5);

		buffer.writeUnsignedVarInt((int) varint);

		Assert.assertEquals(5, buffer.readableBytes());

		buffer.resetReaderIndex();

		Assert.assertEquals(varint, buffer.readUnsignedVarInt());
	}

	/**
	 * Tests the writing and reading of a byte array with the size of the array written as varint.
	 */
	@Test
	public void testByteArray() {
		byte[] array = new byte[]{12, 11, 17, -3, 7, 5};
		PacketBuffer buffer = new PacketBuffer(7);

		buffer.writeByteArray(array);

		Assert.assertEquals(7, buffer.readableBytes());

		buffer.resetReaderIndex();

		Assert.assertEquals(6, buffer.readVarInt());

		buffer.resetReaderIndex();

		Assert.assertArrayEquals(array, buffer.readByteArray());
	}

	/**
	 * Tests the writing and reading of a string with the string being written as byte array.
	 */
	@Test
	public void testString() {
		String text = "This is an example text. It will be encoded into a packet buffer with the UTF-8 charset.";
		PacketBuffer buffer = new PacketBuffer();

		buffer.writeString(text, Charset.forName("UTF-8"));

		Assert.assertEquals(text, buffer.readString(Charset.forName("UTF-8")));
	}

	/**
	 * Tests the compression and decompression of input data using our {@link PacketBuffer}s methods.
	 */
	@Test
	public void testCompressionAndDecompression() {
		String text = "This text will be compressed and decompressed by our PacketBuffer! yeah :D";
		byte[] original = text.getBytes();

		CompressionUtil.init(CompressionUtil.CompressionType.LZ4_FASTEST);

		PacketBuffer buffer = new PacketBuffer();

		buffer.compressAndWriteByteArray(original);

		Assert.assertArrayEquals(original, buffer.readAndDecompress());
	}

}
