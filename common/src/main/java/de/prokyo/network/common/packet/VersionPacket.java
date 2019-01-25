package de.prokyo.network.common.packet;

import de.prokyo.network.common.buffer.PacketBuffer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Used to check the ProkyoNet version.
 */
@AllArgsConstructor
@NoArgsConstructor
public class VersionPacket implements Packet {

	@Getter private byte version;

	@Override
	public void encode(PacketBuffer buffer) {
		buffer.writeByte(this.getVersion());
	}

	@Override
	public void decode(PacketBuffer buffer) {
		this.version = buffer.readByte();
	}

}
