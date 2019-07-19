package de.prokyo.network.common.buffer;

import de.prokyo.network.common.compression.CompressionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

/**
 * An extension of the default {@link ByteBuf} of netty providing the ability to write var integers and strings.
 *
 * <p>VarInt implementation based on the src of
 * <a href="https://github.com/addthis/stream-lib/blob/master/src/main/java/com/clearspring/analytics/util/Varint.java">this</a>.</p>
 *
 */
public class PacketBuffer extends ByteBuf {

	private final ByteBuf buffer;

	/**
	 * Constructs a new Packet Buffer.<br>
	 * The initial capacity is 4.
	 */
	public PacketBuffer() {
		this(4);
	}

	/**
	 * Constructs a new Packet Buffer.
	 *
	 * @param initialCapacity The initial capacity of the packet buffer.
	 */
	public PacketBuffer(int initialCapacity) {
		this(Unpooled.directBuffer(initialCapacity));
	}

	/**
	 * Constructs a new Packet Buffer.
	 *
	 * @param buffer The internal {@link ByteBuf} of the packet buffer.
	 */
	public PacketBuffer(ByteBuf buffer) {
		this.buffer = buffer;
	}

	/**
	 * Compresses the given <i>uncompressedData</i> and writes the compressed data to the buffer including<br>
	 * the uncompressed size.
	 *
	 * <p>First we write the uncompressed size as a VarInt followed by the size of the compressed data as a VarInt.<br>
	 * After these initial values the compressed data will be written to the buffer.</p>
	 *
	 * <p>The {@link CompressionUtil} has to be initialized before calling this method - otherwise u will get a NPE.</p>
	 *
	 * @param uncompressedData The uncompressed data
	 * @return This
	 * @see CompressionUtil#compress(byte[])
	 * @see PacketBuffer#writeVarInt(int)
	 * @see PacketBuffer#writeByteArray(byte[])
	 */
	public PacketBuffer compressAndWriteByteArray(byte[] uncompressedData) {
		byte[] compressedData = CompressionUtil.getInstance().compress(uncompressedData);
		this.writeVarInt(uncompressedData.length);
		this.writeByteArray(compressedData);
		return this;
	}

	/**
	 * Reads the initial values written by {@link PacketBuffer#compressAndWriteByteArray(byte[])}, reads the compressed<br>
	 * data, decompresses the compressed data and returns the uncompressed data.<br>
	 *
	 * <p>The {@link CompressionUtil} has to be initialized before calling this method - otherwise u will get a NPE.</p>
	 *
	 * @return The uncompressed data
	 * @see PacketBuffer#readVarInt()
	 * @see PacketBuffer#readByteArray()
	 * @see CompressionUtil#decompress(byte[], int)
	 * @see PacketBuffer#compressAndWriteByteArray(byte[])
	 */
	public byte[] readAndDecompress() {
		int uncompressedSize = this.readVarInt();
		return CompressionUtil.getInstance().decompress(this.readByteArray(), uncompressedSize);
	}

	/**
	 * Writes the length of the byte array as varint and then the byte array itself into the internal {@link ByteBuf}.
	 *
	 * @param value The byte array that gets written into the packet buffer.
	 * @return the instance of the internal {@link ByteBuf}.
	 * @see #writeVarInt(int)
	 * @see #writeBytes(byte[])
	 */
	public PacketBuffer writeByteArray(byte[] value) {
		this.writeVarInt(value.length);
		return this.writeBytes(value);
	}

	/**
	 * Reads a byte array by first reading a varint and then creating a byte array with the length of the varint.
	 *
	 * @return the read byte array.
	 * @see #readVarInt()
	 * @see #readBytes(byte[])
	 */
	public byte[] readByteArray() {
		byte[] value = new byte[this.readVarInt()];
		this.readBytes(value);
		return value;
	}

	/**
	 * Writes the given int as varint into the internal {@link ByteBuf}.
	 *
	 * @param value The int that gets written into the packet buffer.
	 * @return the instance of the internal {@link ByteBuf}.
	 * @see #writeUnsignedVarInt(int)
	 */
	public PacketBuffer writeVarInt(int value) {
		return this.writeUnsignedVarInt((value << 1) ^ (value >> 31));
	}

	/**
	 * Writes the given int as unsigned varint into the internal {@link ByteBuf}.
	 *
	 * @param value The int that gets written into the packet buffer.
	 * @return the instance of the internal {@link ByteBuf}.
	 * @see #writeByte(int)
	 */
	public PacketBuffer writeUnsignedVarInt(int value) {
		while ((value & 0xFFFFFF80) != 0L) {
			this.writeByte((value & 0x7F) | 0x80);
			value >>>= 7;
		}
		return this.writeByte(value & 0x7F);
	}

	/**
	 * Sets the bytes of the internal {@link ByteBuf} at the given index to the given int as varint.
	 *
	 * @param index The index where the int gets written into the packet buffer.
	 * @param value The int that gets written into the packet buffer.
	 * @return the instance of the internal {@link ByteBuf}.
	 * @see #setUnsignedVarInt(int, int)
	 */
	public PacketBuffer setVarInt(int index, int value) {
		return this.setUnsignedVarInt(index, (value << 1) ^ (value >> 31));
	}

	/**
	 * Sets the bytes of the internal {@link ByteBuf} at the given index to the given int as unsigned varint.
	 *
	 * @param index The index where the int gets written into the packet buffer.
	 * @param value The int that gets written into the packet buffer.
	 * @return the instance of the internal {@link ByteBuf}.
	 * @see #setByte(int, int)
	 */
	public PacketBuffer setUnsignedVarInt(int index, int value) {
		while ((value & 0xFFFFFF80) != 0L) {
			this.setByte(index++, (value & 0x7F) | 0x80);
			value >>>= 7;
		}
		return this.setByte(index, value & 0x7F);
	}

	/**
	 * Reads a varint from the internal {@link ByteBuf}.
	 *
	 * @return The varint as int.
	 * @see #readUnsignedVarInt()
	 */
	public int readVarInt() {
		int raw = (int) this.readUnsignedVarInt();
		int temp = (((raw << 31) >> 31) ^ raw) >> 1;
		return temp ^ (raw & (1 << 31));
	}

	/**
	 * Reads an unsigned varint from the internal {@link ByteBuf}.
	 *
	 * @return The unsigned varint as long.
	 * @see #readByte()
	 */
	public long readUnsignedVarInt() {
		int value = 0;
		int i = 0;
		int b;
		while (((b = this.readByte()) & 0x80) != 0) {
			value |= (b & 0x7F) << i;
			i += 7;
			if (i > 35) {
				throw new IllegalArgumentException("Variable length quantity is too long.");
			}
		}
		return value | (b << i);
	}

	/**
	 * Gets the varint of the internal {@link ByteBuf} at the given index.
	 *
	 * @param index The index where the int gets read from.
	 * @return the varint as int.
	 * @see #getUnsignedVarInt(int)
	 */
	public int getVarInt(int index) {
		int raw = (int) this.getUnsignedVarInt(index);
		int temp = (((raw << 31) >> 31) ^ raw) >> 1;
		return temp ^ (raw & (1 << 31));
	}

	/**
	 * Gets the unsigned varint of the internal {@link ByteBuf} at the given index.
	 *
	 * @param index The index where the int gets read from.
	 * @return the unsigned varint as long.
	 */
	public long getUnsignedVarInt(int index) {
		int value = 0;
		int i = 0;
		int b;
		while (((b = this.getByte(index++)) & 0x80) != 0) {
			value |= (b & 0x7F) << i;
			i += 7;
			if (i > 35) {
				throw new IllegalArgumentException("Variable length quantity is too long.");
			}
		}
		return value | (b << i);
	}

	/**
	 * Writes the given string with the given {@link Charset} into the internal {@link ByteBuf}.
	 *
	 * @param value   The string that gets written into the packet buffer.
	 * @param charset The charset the string is converted into bytes with.
	 * @return the instance of the internal {@link ByteBuf}.
	 */
	public PacketBuffer writeString(String value, Charset charset) {
		byte[] chars = value.getBytes(charset);
		return this.writeByteArray(chars);
	}

	/**
	 * Writes the given string with the <code>UTF-8</code> charset into the internal {@link ByteBuf}.
	 *
	 * @param value The string that gets written into the packet buffer.
	 * @return the instance of the internal {@link ByteBuf}.
	 */
	public PacketBuffer writeString(String value) {
		return this.writeString(value, Charset.forName("UTF-8"));
	}

	/**
	 * Reads a string with the given {@link Charset} from the internal {@link ByteBuf}.
	 *
	 * @param charset The charset the bytes are converted into a string with.
	 * @return the read string.
	 */
	public String readString(Charset charset) {
		byte[] value = this.readByteArray();
		return new String(value, charset);
	}

	/**
	 * Reads a string with the <code>UTF-8</code> charset from the internal {@link ByteBuf}.
	 *
	 * @return the read string.
	 */
	public String readString() {
		return this.readString(Charset.forName("UTF-8"));
	}

	@Override
	public int refCnt() {
		return this.buffer.refCnt();
	}

	@Override
	public boolean release() {
		return this.buffer.release();
	}

	@Override
	public boolean release(int i) {
		return this.buffer.release(i);
	}

	@Override
	public float getFloatLE(int index) {
		return this.buffer.getFloatLE(index);
	}

	@Override
	public double getDoubleLE(int index) {
		return this.buffer.getDoubleLE(index);
	}

	@Override
	public PacketBuffer setFloatLE(int index, float value) {
		this.buffer.setFloatLE(index, value);
		return this;
	}

	@Override
	public PacketBuffer setDoubleLE(int index, double value) {
		this.buffer.setDoubleLE(index, value);
		return this;
	}

	@Override
	public float readFloatLE() {
		return this.buffer.readFloatLE();
	}

	@Override
	public double readDoubleLE() {
		return this.buffer.readDoubleLE();
	}

	@Override
	public PacketBuffer writeFloatLE(float value) {
		this.buffer.writeFloatLE(value);
		return this;
	}

	@Override
	public PacketBuffer writeDoubleLE(double value) {
		this.buffer.writeDoubleLE(value);
		return this;
	}

	@Override
	public int capacity() {
		return this.buffer.capacity();
	}

	@Override
	public PacketBuffer capacity(int newCapacity) {
		this.buffer.capacity(newCapacity);
		return this;
	}

	@Override
	public int maxCapacity() {
		return this.buffer.maxCapacity();
	}

	@Override
	public ByteBufAllocator alloc() {
		return this.buffer.alloc();
	}

	@Deprecated
	@Override
	public ByteOrder order() {
		return this.buffer.order();
	}

	@Deprecated
	@Override
	public ByteBuf order(ByteOrder byteOrder) {
		return this.buffer.order(byteOrder);
	}

	@Override
	public ByteBuf unwrap() {
		return this.buffer.unwrap();
	}

	@Override
	public boolean isDirect() {
		return this.buffer.isDirect();
	}

	@Override
	public boolean isReadOnly() {
		return this.buffer.isReadOnly();
	}

	@Override
	public ByteBuf asReadOnly() {
		return this.buffer.asReadOnly();
	}

	@Override
	public int readerIndex() {
		return this.buffer.readerIndex();
	}

	@Override
	public PacketBuffer readerIndex(int readerIndex) {
		this.buffer.readerIndex(readerIndex);
		return this;
	}

	@Override
	public int writerIndex() {
		return this.buffer.writerIndex();
	}

	@Override
	public PacketBuffer writerIndex(int writeIndex) {
		this.buffer.writerIndex(writeIndex);
		return this;
	}

	@Override
	public PacketBuffer setIndex(int writerIndex, int readerIndex) {
		this.buffer.setIndex(writerIndex, readerIndex);
		return this;
	}

	@Override
	public int readableBytes() {
		return this.buffer.readableBytes();
	}

	@Override
	public int writableBytes() {
		return this.buffer.writableBytes();
	}

	@Override
	public int maxWritableBytes() {
		return this.buffer.maxWritableBytes();
	}

	@Override
	public boolean isReadable() {
		return this.buffer.isReadable();
	}

	@Override
	public boolean isReadable(int size) {
		return this.buffer.isReadable(size);
	}

	@Override
	public boolean isWritable() {
		return this.buffer.isWritable();
	}

	@Override
	public boolean isWritable(int size) {
		return this.buffer.isWritable(size);
	}

	@Override
	public PacketBuffer clear() {
		this.buffer.clear();
		return this;
	}

	@Override
	public PacketBuffer markReaderIndex() {
		this.buffer.markReaderIndex();
		return this;
	}

	@Override
	public PacketBuffer resetReaderIndex() {
		this.buffer.resetReaderIndex();
		return this;
	}

	@Override
	public PacketBuffer markWriterIndex() {
		this.buffer.markWriterIndex();
		return this;
	}

	@Override
	public PacketBuffer resetWriterIndex() {
		this.buffer.resetWriterIndex();
		return this;
	}

	@Override
	public PacketBuffer discardReadBytes() {
		this.buffer.discardReadBytes();
		return this;
	}

	@Override
	public PacketBuffer discardSomeReadBytes() {
		this.buffer.discardSomeReadBytes();
		return this;
	}

	@Override
	public PacketBuffer ensureWritable(int minWritableBytes) {
		this.buffer.ensureWritable(minWritableBytes);
		return this;
	}

	@Override
	public int ensureWritable(int minWritableBytes, boolean force) {
		return this.buffer.ensureWritable(minWritableBytes, force);
	}

	@Override
	public boolean getBoolean(int index) {
		return this.buffer.getBoolean(index);
	}

	@Override
	public byte getByte(int index) {
		return this.buffer.getByte(index);
	}

	@Override
	public short getUnsignedByte(int index) {
		return this.buffer.getUnsignedByte(index);
	}

	@Override
	public short getShort(int index) {
		return this.buffer.getShort(index);
	}

	@Override
	public short getShortLE(int index) {
		return this.buffer.getShortLE(index);
	}

	@Override
	public int getUnsignedShort(int index) {
		return this.buffer.getUnsignedShort(index);
	}

	@Override
	public int getUnsignedShortLE(int index) {
		return this.buffer.getUnsignedShortLE(index);
	}

	@Override
	public int getMedium(int index) {
		return this.buffer.getMedium(index);
	}

	@Override
	public int getMediumLE(int index) {
		return this.buffer.getMediumLE(index);
	}

	@Override
	public int getUnsignedMedium(int index) {
		return this.buffer.getUnsignedMedium(index);
	}

	@Override
	public int getUnsignedMediumLE(int index) {
		return this.buffer.getUnsignedMediumLE(index);
	}

	@Override
	public int getInt(int index) {
		return this.buffer.getInt(index);
	}

	@Override
	public int getIntLE(int index) {
		return this.buffer.getIntLE(index);
	}

	@Override
	public long getUnsignedInt(int index) {
		return this.buffer.getUnsignedInt(index);
	}

	@Override
	public long getUnsignedIntLE(int index) {
		return this.buffer.getUnsignedIntLE(index);
	}

	@Override
	public long getLong(int index) {
		return this.buffer.getLong(index);
	}

	@Override
	public long getLongLE(int index) {
		return this.buffer.getLongLE(index);
	}

	@Override
	public char getChar(int index) {
		return this.buffer.getChar(index);
	}

	@Override
	public float getFloat(int index) {
		return this.buffer.getFloat(index);
	}

	@Override
	public double getDouble(int i) {
		return this.buffer.getDouble(i);
	}

	@Override
	public PacketBuffer getBytes(int index, ByteBuf destination) {
		this.buffer.getBytes(index, destination);
		return this;
	}

	@Override
	public PacketBuffer getBytes(int index, ByteBuf destination, int length) {
		this.buffer.getBytes(index, destination, length);
		return this;
	}

	@Override
	public PacketBuffer getBytes(int index, ByteBuf byteBuf, int destinationIndex, int length) {
		this.buffer.getBytes(index, byteBuf, destinationIndex, length);
		return this;
	}

	@Override
	public PacketBuffer getBytes(int index, byte[] destination) {
		this.buffer.getBytes(index, destination);
		return this;
	}

	@Override
	public PacketBuffer getBytes(int index, byte[] destination, int destinationIndex, int length) {
		this.buffer.getBytes(index, destination, destinationIndex, length);
		return this;
	}

	@Override
	public PacketBuffer getBytes(int index, ByteBuffer destination) {
		this.buffer.getBytes(index, destination);
		return this;
	}

	@Override
	public PacketBuffer getBytes(int index, OutputStream outputStream, int length) throws IOException {
		this.buffer.getBytes(index, outputStream, length);
		return this;
	}

	@Override
	public int getBytes(int index, GatheringByteChannel gatheringByteChannel, int length) throws IOException {
		return this.buffer.getBytes(index, gatheringByteChannel, length);
	}

	@Override
	public int getBytes(int index, FileChannel fileChannel, long position, int length) throws IOException {
		return this.buffer.getBytes(index, fileChannel, position, length);
	}

	@Override
	public CharSequence getCharSequence(int index, int length, Charset charset) {
		return this.buffer.getCharSequence(index, length, charset);
	}

	@Override
	public PacketBuffer setBoolean(int index, boolean value) {
		this.buffer.setBoolean(index, value);
		return this;
	}

	@Override
	public PacketBuffer setByte(int index, int value) {
		this.buffer.setByte(index, value);
		return this;
	}

	@Override
	public PacketBuffer setShort(int index, int value) {
		this.buffer.setShort(index, value);
		return this;
	}

	@Override
	public PacketBuffer setShortLE(int index, int value) {
		this.buffer.setShortLE(index, value);
		return this;
	}

	@Override
	public PacketBuffer setMedium(int index, int value) {
		this.buffer.setMedium(index, value);
		return this;
	}

	@Override
	public PacketBuffer setMediumLE(int index, int value) {
		this.buffer.setMediumLE(index, value);
		return this;
	}

	@Override
	public PacketBuffer setInt(int index, int value) {
		this.buffer.setInt(index, value);
		return this;
	}

	@Override
	public PacketBuffer setIntLE(int index, int value) {
		this.buffer.setIntLE(index, value);
		return this;
	}

	@Override
	public PacketBuffer setLong(int index, long value) {
		this.buffer.setLong(index, value);
		return this;
	}

	@Override
	public PacketBuffer setLongLE(int index, long value) {
		this.buffer.setLongLE(index, value);
		return this;
	}

	@Override
	public PacketBuffer setChar(int index, int value) {
		this.buffer.setChar(index, value);
		return this;
	}

	@Override
	public PacketBuffer setFloat(int index, float value) {
		this.buffer.setFloat(index, value);
		return this;
	}

	@Override
	public PacketBuffer setDouble(int index, double value) {
		this.buffer.setDouble(index, value);
		return this;
	}

	@Override
	public PacketBuffer setBytes(int index, ByteBuf source) {
		this.buffer.setBytes(index, source);
		return this;
	}

	@Override
	public PacketBuffer setBytes(int index, ByteBuf source, int length) {
		this.buffer.setBytes(index, source, length);
		return this;
	}

	@Override
	public PacketBuffer setBytes(int index, ByteBuf source, int sourceIndex, int length) {
		this.buffer.setBytes(index, source, sourceIndex, length);
		return this;
	}

	@Override
	public PacketBuffer setBytes(int index, byte[] source) {
		this.buffer.setBytes(index, source);
		return this;
	}

	@Override
	public PacketBuffer setBytes(int index, byte[] source, int sourceIndex, int length) {
		this.buffer.setBytes(index, source, sourceIndex, length);
		return this;
	}

	@Override
	public PacketBuffer setBytes(int index, ByteBuffer source) {
		this.buffer.setBytes(index, source);
		return this;
	}

	@Override
	public int setBytes(int index, InputStream inputStream, int length) throws IOException {
		return this.buffer.setBytes(index, inputStream, length);
	}

	@Override
	public int setBytes(int index, ScatteringByteChannel scatteringByteChannel, int length) throws IOException {
		return this.buffer.setBytes(index, scatteringByteChannel, length);
	}

	@Override
	public int setBytes(int index, FileChannel fileChannel, long position, int length) throws IOException {
		return this.buffer.setBytes(index, fileChannel, position, length);
	}

	@Override
	public PacketBuffer setZero(int index, int length) {
		this.buffer.setZero(index, length);
		return this;
	}

	@Override
	public int setCharSequence(int index, CharSequence charSequence, Charset charset) {
		return this.buffer.setCharSequence(index, charSequence, charset);
	}

	@Override
	public boolean readBoolean() {
		return this.buffer.readBoolean();
	}

	@Override
	public byte readByte() {
		return this.buffer.readByte();
	}

	@Override
	public short readUnsignedByte() {
		return this.buffer.readUnsignedByte();
	}

	@Override
	public short readShort() {
		return this.buffer.readShort();
	}

	@Override
	public short readShortLE() {
		return this.buffer.readShortLE();
	}

	@Override
	public int readUnsignedShort() {
		return this.buffer.readUnsignedShort();
	}

	@Override
	public int readUnsignedShortLE() {
		return this.buffer.readUnsignedShortLE();
	}

	@Override
	public int readMedium() {
		return this.buffer.readMedium();
	}

	@Override
	public int readMediumLE() {
		return this.buffer.readMediumLE();
	}

	@Override
	public int readUnsignedMedium() {
		return this.buffer.readUnsignedMedium();
	}

	@Override
	public int readUnsignedMediumLE() {
		return this.buffer.readUnsignedMediumLE();
	}

	@Override
	public int readInt() {
		return this.buffer.readInt();
	}

	@Override
	public int readIntLE() {
		return this.buffer.readIntLE();
	}

	@Override
	public long readUnsignedInt() {
		return this.buffer.readUnsignedInt();
	}

	@Override
	public long readUnsignedIntLE() {
		return this.buffer.readUnsignedIntLE();
	}

	@Override
	public long readLong() {
		return this.buffer.readLong();
	}

	@Override
	public long readLongLE() {
		return this.buffer.readLongLE();
	}

	@Override
	public char readChar() {
		return this.buffer.readChar();
	}

	@Override
	public float readFloat() {
		return this.buffer.readFloat();
	}

	@Override
	public double readDouble() {
		return this.buffer.readDouble();
	}

	@Override
	public ByteBuf readBytes(int length) {
		return this.buffer.readBytes(length);
	}

	@Override
	public ByteBuf readSlice(int length) {
		return this.buffer.readSlice(length);
	}

	@Override
	public ByteBuf readRetainedSlice(int length) {
		return this.buffer.readRetainedSlice(length);
	}

	@Override
	public PacketBuffer readBytes(ByteBuf destination) {
		this.buffer.readBytes(destination);
		return this;
	}

	@Override
	public PacketBuffer readBytes(ByteBuf destination, int length) {
		this.buffer.readBytes(destination, length);
		return this;
	}

	@Override
	public PacketBuffer readBytes(ByteBuf destination, int destinationIndex, int length) {
		this.buffer.readBytes(destination, destinationIndex, length);
		return this;
	}

	@Override
	public PacketBuffer readBytes(byte[] destination) {
		this.buffer.readBytes(destination);
		return this;
	}

	@Override
	public PacketBuffer readBytes(byte[] destination, int destinationIndex, int length) {
		this.buffer.readBytes(destination, destinationIndex, length);
		return this;
	}

	@Override
	public PacketBuffer readBytes(ByteBuffer destination) {
		this.buffer.readBytes(destination);
		return this;
	}

	@Override
	public PacketBuffer readBytes(OutputStream outputStream, int length) throws IOException {
		this.buffer.readBytes(outputStream, length);
		return this;
	}

	@Override
	public int readBytes(GatheringByteChannel gatheringByteChannel, int length) throws IOException {
		return this.buffer.readBytes(gatheringByteChannel, length);
	}

	@Override
	public int readBytes(FileChannel fileChannel, long position, int length) throws IOException {
		return this.buffer.readBytes(fileChannel, position, length);
	}

	@Override
	public CharSequence readCharSequence(int length, Charset charset) {
		return this.buffer.readCharSequence(length, charset);
	}

	@Override
	public PacketBuffer skipBytes(int length) {
		this.buffer.skipBytes(length);
		return this;
	}

	@Override
	public PacketBuffer writeBoolean(boolean value) {
		this.buffer.writeBoolean(value);
		return this;
	}

	@Override
	public PacketBuffer writeByte(int value) {
		this.buffer.writeByte(value);
		return this;
	}

	@Override
	public PacketBuffer writeShort(int value) {
		this.buffer.writeShort(value);
		return this;
	}

	@Override
	public PacketBuffer writeShortLE(int value) {
		this.buffer.writeShortLE(value);
		return this;
	}

	@Override
	public PacketBuffer writeMedium(int value) {
		this.buffer.writeMedium(value);
		return this;
	}

	@Override
	public PacketBuffer writeMediumLE(int value) {
		this.buffer.writeMediumLE(value);
		return this;
	}

	@Override
	public PacketBuffer writeInt(int value) {
		this.buffer.writeInt(value);
		return this;
	}

	@Override
	public PacketBuffer writeIntLE(int value) {
		this.buffer.writeIntLE(value);
		return this;
	}

	@Override
	public PacketBuffer writeLong(long value) {
		this.buffer.writeLong(value);
		return this;
	}

	@Override
	public PacketBuffer writeLongLE(long value) {
		this.buffer.writeLongLE(value);
		return this;
	}

	@Override
	public PacketBuffer writeChar(int value) {
		this.buffer.writeChar(value);
		return this;
	}

	@Override
	public PacketBuffer writeFloat(float value) {
		this.buffer.writeFloat(value);
		return this;
	}

	@Override
	public PacketBuffer writeDouble(double value) {
		this.buffer.writeDouble(value);
		return this;
	}

	@Override
	public PacketBuffer writeBytes(ByteBuf source) {
		this.buffer.writeBytes(source);
		return this;
	}

	@Override
	public PacketBuffer writeBytes(ByteBuf source, int length) {
		this.buffer.writeBytes(source, length);
		return this;
	}

	@Override
	public PacketBuffer writeBytes(ByteBuf source, int sourceIndex, int length) {
		this.buffer.writeBytes(source, sourceIndex, length);
		return this;
	}

	@Override
	public PacketBuffer writeBytes(byte[] source) {
		this.buffer.writeBytes(source);
		return this;
	}

	@Override
	public PacketBuffer writeBytes(byte[] source, int sourceIndex, int length) {
		this.buffer.writeBytes(source, sourceIndex, length);
		return this;
	}

	@Override
	public PacketBuffer writeBytes(ByteBuffer source) {
		this.buffer.writeBytes(source);
		return this;
	}

	@Override
	public int writeBytes(InputStream inputStream, int length) throws IOException {
		return this.buffer.writeBytes(inputStream, length);
	}

	@Override
	public int writeBytes(ScatteringByteChannel scatteringByteChannel, int length) throws IOException {
		return this.buffer.writeBytes(scatteringByteChannel, length);
	}

	@Override
	public int writeBytes(FileChannel fileChannel, long position, int length) throws IOException {
		return this.buffer.writeBytes(fileChannel, position, length);
	}

	@Override
	public PacketBuffer writeZero(int length) {
		this.buffer.writeZero(length);
		return this;
	}

	@Override
	public int writeCharSequence(CharSequence charSequence, Charset charset) {
		return this.buffer.writeCharSequence(charSequence, charset);
	}

	@Override
	public int indexOf(int fromIndex, int toIndex, byte value) {
		return this.buffer.indexOf(fromIndex, toIndex, value);
	}

	@Override
	public int bytesBefore(byte value) {
		return this.buffer.bytesBefore(value);
	}

	@Override
	public int bytesBefore(int length, byte value) {
		return this.buffer.bytesBefore(length, value);
	}

	@Override
	public int bytesBefore(int index, int length, byte value) {
		return this.buffer.bytesBefore(index, length, value);
	}

	@Override
	public int forEachByte(ByteProcessor byteProcessor) {
		return this.buffer.forEachByte(byteProcessor);
	}

	@Override
	public int forEachByte(int index, int length, ByteProcessor byteProcessor) {
		return this.buffer.forEachByte(index, length, byteProcessor);
	}

	@Override
	public int forEachByteDesc(ByteProcessor byteProcessor) {
		return this.buffer.forEachByteDesc(byteProcessor);
	}

	@Override
	public int forEachByteDesc(int index, int length, ByteProcessor byteProcessor) {
		return this.buffer.forEachByteDesc(index, length, byteProcessor);
	}

	@Override
	public PacketBuffer copy() {
		return new PacketBuffer(this.buffer.copy());
	}

	@Override
	public PacketBuffer copy(int index, int length) {
		return new PacketBuffer(this.buffer.copy(index, length));
	}

	@Override
	public PacketBuffer slice() {
		return new PacketBuffer(this.buffer.slice());
	}

	@Override
	public PacketBuffer retainedSlice() {
		return new PacketBuffer(this.buffer.retainedSlice());
	}

	@Override
	public PacketBuffer slice(int index, int length) {
		return new PacketBuffer(this.buffer.slice(index, length));
	}

	@Override
	public PacketBuffer retainedSlice(int index, int length) {
		return new PacketBuffer(this.buffer.retainedSlice(index, length));
	}

	@Override
	public PacketBuffer duplicate() {
		return new PacketBuffer(this.buffer.duplicate());
	}

	@Override
	public PacketBuffer retainedDuplicate() {
		return new PacketBuffer(this.buffer.retainedDuplicate());
	}

	@Override
	public int nioBufferCount() {
		return this.buffer.nioBufferCount();
	}

	@Override
	public ByteBuffer nioBuffer() {
		return this.buffer.nioBuffer();
	}

	@Override
	public ByteBuffer nioBuffer(int index, int length) {
		return this.buffer.nioBuffer(index, length);
	}

	@Override
	public ByteBuffer internalNioBuffer(int index, int length) {
		return this.buffer.internalNioBuffer(index, length);
	}

	@Override
	public ByteBuffer[] nioBuffers() {
		return this.buffer.nioBuffers();
	}

	@Override
	public ByteBuffer[] nioBuffers(int index, int length) {
		return this.buffer.nioBuffers(index, length);
	}

	@Override
	public boolean hasArray() {
		return this.buffer.hasArray();
	}

	/**
	 * Returns the data this buffer contains.<br/>
	 *
	 * This method creates a new byte array and writes the data into the byte array.<br/>
	 * The reader index won't change.
	 *
	 * @see {{@link ByteBuf#array()}}
	 * @return The data this buffer contains
	 */
	@Override
	public byte[] array() {
		// workaround by prokyo - direct buffers sometimes don't support array()

		// save the original reader index to 'pointer' and set the readerIndex of the buffer to the start
		int pointer = this.readerIndex();
		this.buffer.readerIndex(0);

		// read all available bytes
		byte[] data = new byte[this.readableBytes()];
		this.readBytes(data);

		// reset the reader index to the previous pointer
		this.buffer.readerIndex(pointer);

		return data;
	}

	@Override
	public int arrayOffset() {
		return this.buffer.arrayOffset();
	}

	@Override
	public boolean hasMemoryAddress() {
		return this.buffer.hasMemoryAddress();
	}

	@Override
	public long memoryAddress() {
		return this.buffer.memoryAddress();
	}

	@Override
	public String toString(Charset charset) {
		return this.buffer.toString(charset);
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		return this.buffer.toString(index, length, charset);
	}

	@Override
	public int hashCode() {
		return this.buffer.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this.buffer.equals(o);
	}

	@Override
	public int compareTo(ByteBuf byteBuf) {
		return this.buffer.compareTo(byteBuf);
	}

	@Override
	public String toString() {
		return this.buffer.toString();
	}

	@Override
	public PacketBuffer retain(int increment) {
		this.buffer.retain(increment);
		return this;
	}

	@Override
	public PacketBuffer retain() {
		this.buffer.retain();
		return this;
	}

	@Override
	public PacketBuffer touch() {
		this.buffer.touch();
		return this;
	}

	@Override
	public PacketBuffer touch(Object hint) {
		this.buffer.touch(hint);
		return this;
	}

}
