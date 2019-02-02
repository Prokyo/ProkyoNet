package de.prokyo.network.common.compression;

import lombok.Getter;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Decompressor;
import net.jpountz.lz4.LZ4Factory;

/**
 * Represents a util for compressing and decompressing generic data.
 */
public class CompressionUtil {

	@Getter private static CompressionUtil instance;

	private LZ4Factory lz4Factory;
	private LZ4Compressor lz4Compressor;
	private LZ4Decompressor lz4Decompressor;

	/**
	 * Constructor.
	 *
	 * @param type The type and strength of the compression
	 */
	public CompressionUtil(CompressionType type) {
		switch (type) {
			case LZ4_FASTEST:
				this.lz4Factory = LZ4Factory.fastestInstance();
				this.lz4Compressor = this.lz4Factory.fastCompressor();
				this.lz4Decompressor = this.lz4Factory.fastDecompressor();
				break;

			default:
				break;
		}
	}

	/**
	 * Compresses the given <i>uncompressedData</i> and returns the compressed data.
	 *
	 * @param uncompressedData The uncompressed input data
	 * @return The compressed output data
	 */
	public byte[] compress(byte[] uncompressedData) {
		// get the max compressed size and create an array with the max compressed size
		int maxCompressedLength = this.lz4Compressor.maxCompressedLength(uncompressedData.length);
		byte[] compressedData = new byte[maxCompressedLength];

		// compress the input into the compressedData array and save the needed bytes in bytesUsed for shrinking the
		// maximal array to an array with the length of the actually used bytes
		int bytesUsed = this.lz4Compressor.compress(uncompressedData, compressedData);

		// shrink the compressed data to it's minimal size
		byte[] minCompressedData = new byte[bytesUsed];
		System.arraycopy(compressedData, 0, minCompressedData, 0, bytesUsed);

		return minCompressedData;
	}

	/**
	 * Decompresses the given <i>compressedData</i> with given <i>uncompressedSize</i> and returns the corresponding<br>
	 * uncompressed data.
	 *
	 * @param compressedData The compressed data
	 * @param uncompressedSize The size of the original uncompressed data
	 * @return The uncompressed data
	 */
	public byte[] decompress(byte[] compressedData, int uncompressedSize) {
		byte[] uncompressedData = new byte[uncompressedSize];
		this.lz4Decompressor.decompress(compressedData, 0, uncompressedData, 0, uncompressedSize);
		return uncompressedData;
	}

	/**
	 * Initializes the singleton instance with given arguments.
	 *
	 * @param type The type of the compression algorithm used
	 */
	public static void init(CompressionType type) {
		CompressionUtil.instance = new CompressionUtil(type);
	}

	/**
	 * The {@link CompressionType} defines the strength and type of the compression.
	 */
	public enum CompressionType {
		LZ4_FASTEST
	}

}
