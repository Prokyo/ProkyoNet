package de.prokyo.network;

import de.prokyo.network.common.buffer.PacketBuffer;
import de.prokyo.network.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Test packet to test if packets are being encoded and decoded.
 */
@AllArgsConstructor
@NoArgsConstructor
public class PingPacket implements Packet {

	private Sender sender;
	@Getter private long time;

	@Override
	public void encode(PacketBuffer buffer) {
		buffer.writeVarInt(this.sender.ordinal());
		buffer.writeLong(this.time);
	}

	@Override
	public void decode(PacketBuffer buffer) {
		this.sender = Sender.fromId(buffer.readVarInt());
		if (this.sender == Sender.CLIENT) ConnectionTest.serverReceived = true;
		else if (this.sender == Sender.SERVER) ConnectionTest.clientReceived = true;
		this.time = buffer.readLong();
	}

	enum Sender {
		CLIENT,
		SERVER;

		public static Sender fromId(int id) {
			switch (id) {
				case 0:
					return CLIENT;

				case 1:
					return SERVER;

				default:
					return null;
			}
		}
	}

}
