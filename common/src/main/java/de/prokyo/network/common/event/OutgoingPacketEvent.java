package de.prokyo.network.common.event;

import de.prokyo.network.common.packet.Packet;
import lombok.Data;

/**
 * Event that will be called when a packet is written to the pipeline. (flushing ignored)
 */
@Data
public class OutgoingPacketEvent implements Event {

	private final Packet packet;

}
