package de.prokyo.network.common.packet;

import de.prokyo.network.common.buffer.PacketBuffer;

/**
 * Represents a packet containing a bunch of information.
 */
public interface Packet {

	/**
	 * Encodes the information and writes it to the given buffer.
	 *
	 * @param buffer A packet buffer which will contain the encoded data
	 */
	void encode(PacketBuffer buffer);

	/**
	 * Decodes the information of the given buffer and sets it to the variables of the implementation.
	 *
	 * @param buffer A packet buffer containing the encoded data
	 */
	void decode(PacketBuffer buffer);
}
