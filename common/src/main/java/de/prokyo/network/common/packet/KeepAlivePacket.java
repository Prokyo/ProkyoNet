package de.prokyo.network.common.packet;

import de.prokyo.network.common.buffer.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Packet for keeping the connection alive.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeepAlivePacket implements Packet {

	private long timestamp;
	private boolean forwarded; // the sender sets this to false and the forwarder sets this to true

	@Override
	public void encode(PacketBuffer buffer) {
		buffer.writeLong(timestamp);
		buffer.writeBoolean(forwarded);
	}

	@Override
	public void decode(PacketBuffer buffer) {
		this.timestamp = buffer.readLong();
		this.forwarded = buffer.readBoolean();
	}
}
