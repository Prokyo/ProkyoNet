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
	 *
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
	 * the uncompressed size.<br>
	 * First we write the uncompressed size as a VarInt followed by the size of the compressed data as a VarInt.<br>
	 * After these initial values the compressed data will be written to the buffer.<br>
	 *
	 * The {@link CompressionUtil} has to be initialized before calling this method - otherwise u will get a NPE.
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
	 * The {@link CompressionUtil} has to be initialized before calling this method - otherwise u will get a NPE.
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
	public ByteBuf writeByteArray(byte[] value) {
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
	public ByteBuf writeVarInt(int value) {
		return this.writeUnsignedVarInt((value << 1) ^ (value >> 31));
	}

	/**
	 * Writes the given int as unsigned varint into the internal {@link ByteBuf}.
	 *
	 * @param value The int that gets written into the packet buffer.
	 * @return the instance of the internal {@link ByteBuf}.
	 * @see #writeByte(int)
	 */
	public ByteBuf writeUnsignedVarInt(int value) {
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
	public ByteBuf setVarInt(int index, int value) {
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
	public ByteBuf setUnsignedVarInt(int index, int value) {
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
	public ByteBuf writeString(String value, Charset charset) {
		byte[] chars = value.getBytes(charset);
		return this.writeByteArray(chars);
	}

	/**
	 * Writes the given string with the <code>UTF-8</code> charset into the internal {@link ByteBuf}.
	 *
	 * @param value The string that gets written into the packet buffer.
	 * @return the instance of the internal {@link ByteBuf}.
	 */
	public ByteBuf writeString(String value) {
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
	public ByteBuf setFloatLE(int index, float value) {
		return this.buffer.setFloatLE(index, value);
	}

	@Override
	public ByteBuf setDoubleLE(int index, double value) {
		return this.buffer.setDoubleLE(index, value);
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
	public ByteBuf writeFloatLE(float value) {
		return this.buffer.writeFloatLE(value);
	}

	@Override
	public ByteBuf writeDoubleLE(double value) {
		return this.buffer.writeDoubleLE(value);
	}

	@Override
	public int capacity() {
		return this.buffer.capacity();
	}

	@Override
	public ByteBuf capacity(int i) {
		return this.buffer.capacity(i);
	}

	@Override
	public int maxCapacity() {
		return this.buffer.maxCapacity();
	}

	@Override
	public ByteBufAllocator alloc() {
		return this.buffer.alloc();
	}

	@Override
	public ByteOrder order() {
		return this.buffer.order();
	}

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
	public ByteBuf readerIndex(int i) {
		return this.buffer.readerIndex(i);
	}

	@Override
	public int writerIndex() {
		return this.buffer.writerIndex();
	}

	@Override
	public ByteBuf writerIndex(int i) {
		return this.buffer.writerIndex(i);
	}

	@Override
	public ByteBuf setIndex(int i, int i1) {
		return this.buffer.setIndex(i, i1);
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
	public boolean isReadable(int i) {
		return this.buffer.isReadable(i);
	}

	@Override
	public boolean isWritable() {
		return this.buffer.isWritable();
	}

	@Override
	public boolean isWritable(int i) {
		return this.buffer.isWritable(i);
	}

	@Override
	public ByteBuf clear() {
		return this.buffer.clear();
	}

	@Override
	public ByteBuf markReaderIndex() {
		return this.buffer.markReaderIndex();
	}

	@Override
	public ByteBuf resetReaderIndex() {
		return this.buffer.resetReaderIndex();
	}

	@Override
	public ByteBuf markWriterIndex() {
		return this.buffer.markWriterIndex();
	}

	@Override
	public ByteBuf resetWriterIndex() {
		return this.buffer.resetWriterIndex();
	}

	@Override
	public ByteBuf discardReadBytes() {
		return this.buffer.discardReadBytes();
	}

	@Override
	public ByteBuf discardSomeReadBytes() {
		return this.buffer.discardSomeReadBytes();
	}

	@Override
	public ByteBuf ensureWritable(int i) {
		return this.buffer.ensureWritable(i);
	}

	@Override
	public int ensureWritable(int i, boolean b) {
		return this.buffer.ensureWritable(i, b);
	}

	@Override
	public boolean getBoolean(int i) {
		return this.buffer.getBoolean(i);
	}

	@Override
	public byte getByte(int i) {
		return this.buffer.getByte(i);
	}

	@Override
	public short getUnsignedByte(int i) {
		return this.buffer.getUnsignedByte(i);
	}

	@Override
	public short getShort(int i) {
		return this.buffer.getShort(i);
	}

	@Override
	public short getShortLE(int i) {
		return this.buffer.getShortLE(i);
	}

	@Override
	public int getUnsignedShort(int i) {
		return this.buffer.getUnsignedShort(i);
	}

	@Override
	public int getUnsignedShortLE(int i) {
		return this.buffer.getUnsignedShortLE(i);
	}

	@Override
	public int getMedium(int i) {
		return this.buffer.getMedium(i);
	}

	@Override
	public int getMediumLE(int i) {
		return this.buffer.getMediumLE(i);
	}

	@Override
	public int getUnsignedMedium(int i) {
		return this.buffer.getUnsignedMedium(i);
	}

	@Override
	public int getUnsignedMediumLE(int i) {
		return this.buffer.getUnsignedMediumLE(i);
	}

	@Override
	public int getInt(int i) {
		return this.buffer.getInt(i);
	}

	@Override
	public int getIntLE(int i) {
		return this.buffer.getIntLE(i);
	}

	@Override
	public long getUnsignedInt(int i) {
		return this.buffer.getUnsignedInt(i);
	}

	@Override
	public long getUnsignedIntLE(int i) {
		return this.buffer.getUnsignedIntLE(i);
	}

	@Override
	public long getLong(int i) {
		return this.buffer.getLong(i);
	}

	@Override
	public long getLongLE(int i) {
		return this.buffer.getLongLE(i);
	}

	@Override
	public char getChar(int i) {
		return this.buffer.getChar(i);
	}

	@Override
	public float getFloat(int i) {
		return this.buffer.getFloat(i);
	}

	@Override
	public double getDouble(int i) {
		return this.buffer.getDouble(i);
	}

	@Override
	public ByteBuf getBytes(int i, ByteBuf byteBuf) {
		return this.buffer.getBytes(i, byteBuf);
	}

	@Override
	public ByteBuf getBytes(int i, ByteBuf byteBuf, int i1) {
		return this.buffer.getBytes(i, byteBuf, i1);
	}

	@Override
	public ByteBuf getBytes(int i, ByteBuf byteBuf, int i1, int i2) {
		return this.buffer.getBytes(i, byteBuf, i1, i2);
	}

	@Override
	public ByteBuf getBytes(int i, byte[] bytes) {
		return this.buffer.getBytes(i, bytes);
	}

	@Override
	public ByteBuf getBytes(int i, byte[] bytes, int i1, int i2) {
		return this.buffer.getBytes(i, bytes, i1, i2);
	}

	@Override
	public ByteBuf getBytes(int i, ByteBuffer byteBuffer) {
		return this.buffer.getBytes(i, byteBuffer);
	}

	@Override
	public ByteBuf getBytes(int i, OutputStream outputStream, int i1) throws IOException {
		return this.buffer.getBytes(i, outputStream, i1);
	}

	@Override
	public int getBytes(int i, GatheringByteChannel gatheringByteChannel, int i1) throws IOException {
		return this.buffer.getBytes(i, gatheringByteChannel, i1);
	}

	@Override
	public int getBytes(int i, FileChannel fileChannel, long l, int i1) throws IOException {
		return this.buffer.getBytes(i, fileChannel, l, i1);
	}

	@Override
	public CharSequence getCharSequence(int i, int i1, Charset charset) {
		return this.buffer.getCharSequence(i, i1, charset);
	}

	@Override
	public ByteBuf setBoolean(int i, boolean b) {
		return this.buffer.setBoolean(i, b);
	}

	@Override
	public ByteBuf setByte(int i, int i1) {
		return this.buffer.setByte(i, i1);
	}

	@Override
	public ByteBuf setShort(int i, int i1) {
		return this.buffer.setShort(i, i1);
	}

	@Override
	public ByteBuf setShortLE(int i, int i1) {
		return this.buffer.setShortLE(i, i1);
	}

	@Override
	public ByteBuf setMedium(int i, int i1) {
		return this.buffer.setMedium(i, i1);
	}

	@Override
	public ByteBuf setMediumLE(int i, int i1) {
		return this.buffer.setMediumLE(i, i1);
	}

	@Override
	public ByteBuf setInt(int i, int i1) {
		return this.buffer.setInt(i, i1);
	}

	@Override
	public ByteBuf setIntLE(int i, int i1) {
		return this.buffer.setIntLE(i, i1);
	}

	@Override
	public ByteBuf setLong(int i, long l) {
		return this.buffer.setLong(i, l);
	}

	@Override
	public ByteBuf setLongLE(int i, long l) {
		return this.buffer.setLongLE(i, l);
	}

	@Override
	public ByteBuf setChar(int i, int i1) {
		return this.buffer.setChar(i, i1);
	}

	@Override
	public ByteBuf setFloat(int i, float v) {
		return this.buffer.setFloat(i, v);
	}

	@Override
	public ByteBuf setDouble(int i, double v) {
		return this.buffer.setDouble(i, v);
	}

	@Override
	public ByteBuf setBytes(int i, ByteBuf byteBuf) {
		return this.buffer.setBytes(i, byteBuf);
	}

	@Override
	public ByteBuf setBytes(int i, ByteBuf byteBuf, int i1) {
		return this.buffer.setBytes(i, byteBuf, i1);
	}

	@Override
	public ByteBuf setBytes(int i, ByteBuf byteBuf, int i1, int i2) {
		return this.buffer.setBytes(i, byteBuf, i1, i2);
	}

	@Override
	public ByteBuf setBytes(int i, byte[] bytes) {
		return this.buffer.setBytes(i, bytes);
	}

	@Override
	public ByteBuf setBytes(int i, byte[] bytes, int i1, int i2) {
		return this.buffer.setBytes(i, bytes, i1, i2);
	}

	@Override
	public ByteBuf setBytes(int i, ByteBuffer byteBuffer) {
		return this.buffer.setBytes(i, byteBuffer);
	}

	@Override
	public int setBytes(int i, InputStream inputStream, int i1) throws IOException {
		return this.buffer.setBytes(i, inputStream, i1);
	}

	@Override
	public int setBytes(int i, ScatteringByteChannel scatteringByteChannel, int i1) throws IOException {
		return this.buffer.setBytes(i, scatteringByteChannel, i1);
	}

	@Override
	public int setBytes(int i, FileChannel fileChannel, long l, int i1) throws IOException {
		return this.buffer.setBytes(i, fileChannel, l, i1);
	}

	@Override
	public ByteBuf setZero(int i, int i1) {
		return this.buffer.setZero(i, i1);
	}

	@Override
	public int setCharSequence(int i, CharSequence charSequence, Charset charset) {
		return this.buffer.setCharSequence(i, charSequence, charset);
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
	public ByteBuf readBytes(int i) {
		return this.buffer.readBytes(i);
	}

	@Override
	public ByteBuf readSlice(int i) {
		return this.buffer.readSlice(i);
	}

	@Override
	public ByteBuf readRetainedSlice(int i) {
		return this.buffer.readRetainedSlice(i);
	}

	@Override
	public ByteBuf readBytes(ByteBuf byteBuf) {
		return this.buffer.readBytes(byteBuf);
	}

	@Override
	public ByteBuf readBytes(ByteBuf byteBuf, int i) {
		return this.buffer.readBytes(byteBuf, i);
	}

	@Override
	public ByteBuf readBytes(ByteBuf byteBuf, int i, int i1) {
		return this.buffer.readBytes(byteBuf, i, i1);
	}

	@Override
	public ByteBuf readBytes(byte[] bytes) {
		return this.buffer.readBytes(bytes);
	}

	@Override
	public ByteBuf readBytes(byte[] bytes, int i, int i1) {
		return this.buffer.readBytes(bytes, i, i1);
	}

	@Override
	public ByteBuf readBytes(ByteBuffer byteBuffer) {
		return this.buffer.readBytes(byteBuffer);
	}

	@Override
	public ByteBuf readBytes(OutputStream outputStream, int i) throws IOException {
		return this.buffer.readBytes(outputStream, i);
	}

	@Override
	public int readBytes(GatheringByteChannel gatheringByteChannel, int i) throws IOException {
		return this.buffer.readBytes(gatheringByteChannel, i);
	}

	@Override
	public int readBytes(FileChannel fileChannel, long l, int i) throws IOException {
		return this.buffer.readBytes(fileChannel, l, i);
	}

	@Override
	public CharSequence readCharSequence(int i, Charset charset) {
		return this.buffer.readCharSequence(i, charset);
	}

	@Override
	public ByteBuf skipBytes(int i) {
		return this.buffer.skipBytes(i);
	}

	@Override
	public ByteBuf writeBoolean(boolean b) {
		return this.buffer.writeBoolean(b);
	}

	@Override
	public ByteBuf writeByte(int i) {
		return this.buffer.writeByte(i);
	}

	@Override
	public ByteBuf writeShort(int i) {
		return this.buffer.writeShort(i);
	}

	@Override
	public ByteBuf writeShortLE(int i) {
		return this.buffer.writeShortLE(i);
	}

	@Override
	public ByteBuf writeMedium(int i) {
		return this.buffer.writeMedium(i);
	}

	@Override
	public ByteBuf writeMediumLE(int i) {
		return this.buffer.writeMediumLE(i);
	}

	@Override
	public ByteBuf writeInt(int i) {
		return this.buffer.writeInt(i);
	}

	@Override
	public ByteBuf writeIntLE(int i) {
		return this.buffer.writeIntLE(i);
	}

	@Override
	public ByteBuf writeLong(long l) {
		return this.buffer.writeLong(l);
	}

	@Override
	public ByteBuf writeLongLE(long l) {
		return this.buffer.writeLongLE(l);
	}

	@Override
	public ByteBuf writeChar(int i) {
		return this.buffer.writeChar(i);
	}

	@Override
	public ByteBuf writeFloat(float v) {
		return this.buffer.writeFloat(v);
	}

	@Override
	public ByteBuf writeDouble(double v) {
		return this.buffer.writeDouble(v);
	}

	@Override
	public ByteBuf writeBytes(ByteBuf byteBuf) {
		return this.buffer.writeBytes(byteBuf);
	}

	@Override
	public ByteBuf writeBytes(ByteBuf byteBuf, int i) {
		return this.buffer.writeBytes(byteBuf, i);
	}

	@Override
	public ByteBuf writeBytes(ByteBuf byteBuf, int i, int i1) {
		return this.buffer.writeBytes(byteBuf, i, i1);
	}

	@Override
	public ByteBuf writeBytes(byte[] bytes) {
		return this.buffer.writeBytes(bytes);
	}

	@Override
	public ByteBuf writeBytes(byte[] bytes, int i, int i1) {
		return this.buffer.writeBytes(bytes, i, i1);
	}

	@Override
	public ByteBuf writeBytes(ByteBuffer byteBuffer) {
		return this.buffer.writeBytes(byteBuffer);
	}

	@Override
	public int writeBytes(InputStream inputStream, int i) throws IOException {
		return this.buffer.writeBytes(inputStream, i);
	}

	@Override
	public int writeBytes(ScatteringByteChannel scatteringByteChannel, int i) throws IOException {
		return this.buffer.writeBytes(scatteringByteChannel, i);
	}

	@Override
	public int writeBytes(FileChannel fileChannel, long l, int i) throws IOException {
		return this.buffer.writeBytes(fileChannel, l, i);
	}

	@Override
	public ByteBuf writeZero(int i) {
		return this.buffer.writeZero(i);
	}

	@Override
	public int writeCharSequence(CharSequence charSequence, Charset charset) {
		return this.buffer.writeCharSequence(charSequence, charset);
	}

	@Override
	public int indexOf(int i, int i1, byte b) {
		return this.buffer.indexOf(i, i1, b);
	}

	@Override
	public int bytesBefore(byte b) {
		return this.buffer.bytesBefore(b);
	}

	@Override
	public int bytesBefore(int i, byte b) {
		return this.buffer.bytesBefore(i, b);
	}

	@Override
	public int bytesBefore(int i, int i1, byte b) {
		return this.buffer.bytesBefore(i, i1, b);
	}

	@Override
	public int forEachByte(ByteProcessor byteProcessor) {
		return this.buffer.forEachByte(byteProcessor);
	}

	@Override
	public int forEachByte(int i, int i1, ByteProcessor byteProcessor) {
		return this.buffer.forEachByte(i, i1, byteProcessor);
	}

	@Override
	public int forEachByteDesc(ByteProcessor byteProcessor) {
		return this.buffer.forEachByteDesc(byteProcessor);
	}

	@Override
	public int forEachByteDesc(int i, int i1, ByteProcessor byteProcessor) {
		return this.buffer.forEachByteDesc(i, i1, byteProcessor);
	}

	@Override
	public ByteBuf copy() {
		return this.buffer.copy();
	}

	@Override
	public ByteBuf copy(int i, int i1) {
		return this.buffer.copy(i, i1);
	}

	@Override
	public ByteBuf slice() {
		return this.buffer.slice();
	}

	@Override
	public ByteBuf retainedSlice() {
		return this.buffer.retainedSlice();
	}

	@Override
	public ByteBuf slice(int i, int i1) {
		return this.buffer.slice(i, i1);
	}

	@Override
	public ByteBuf retainedSlice(int i, int i1) {
		return this.buffer.retainedSlice(i, i1);
	}

	@Override
	public ByteBuf duplicate() {
		return this.buffer.duplicate();
	}

	@Override
	public ByteBuf retainedDuplicate() {
		return this.buffer.retainedDuplicate();
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
	public ByteBuffer nioBuffer(int i, int i1) {
		return this.buffer.nioBuffer(i, i1);
	}

	@Override
	public ByteBuffer internalNioBuffer(int i, int i1) {
		return this.buffer.internalNioBuffer(i, i1);
	}

	@Override
	public ByteBuffer[] nioBuffers() {
		return this.buffer.nioBuffers();
	}

	@Override
	public ByteBuffer[] nioBuffers(int i, int i1) {
		return this.buffer.nioBuffers(i, i1);
	}

	@Override
	public boolean hasArray() {
		return this.buffer.hasArray();
	}

	@Override
	public byte[] array() {
		return this.buffer.array();
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
	public String toString(int i, int i1, Charset charset) {
		return this.buffer.toString(i, i1, charset);
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
	public ByteBuf retain(int i) {
		return this.buffer.retain(i);
	}

	@Override
	public ByteBuf retain() {
		return this.buffer.retain();
	}

	@Override
	public ByteBuf touch() {
		return this.buffer.touch();
	}

	@Override
	public ByteBuf touch(Object o) {
		return this.buffer.touch(o);
	}
}