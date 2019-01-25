package de.prokyo.network.common.connection;

import de.prokyo.network.common.packet.Packet;
import de.prokyo.network.common.packet.PacketRegistry;
import de.prokyo.network.common.packet.VersionPacket;

/**
 * Represents a connection over the internet.<br>
 * It's not important whether the remote host is a client or a server instance.
 */
public interface Connection {

	/**
	 * Sends the given packet to the remote host.
	 *
	 * @param packet The packet which shall be sent to the remote host.
	 */
	void sendPacket(Packet packet);

	/**
	 * Sends the version of the currently used ProkyoNet protocol (reserved packets).
	 */
	default void sendVersionInformation() {
		this.sendPacket(new VersionPacket(PacketRegistry.getProkyoProtocolVersion()));
	}

}
