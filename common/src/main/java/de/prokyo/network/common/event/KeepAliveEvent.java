package de.prokyo.network.common.event;

import de.prokyo.network.common.connection.Connection;
import de.prokyo.network.common.packet.KeepAlivePacket;
import io.netty.channel.Channel;
import lombok.*;

/**
 * Event that will be triggered when a (forwarded) KeepAlivePacket was received.
 */
@Data
public class KeepAliveEvent implements Event {

	private final KeepAlivePacket packet;
	@Setter(AccessLevel.PROTECTED) private Connection connection;
	private final Unsafe unsafe = new Unsafe(this);
	@Getter(AccessLevel.PROTECTED) private final Channel channel;

	/**
	 * Unsafe
	 */
	@AllArgsConstructor
	public class Unsafe {

		private final KeepAliveEvent event;

		/**
		 * Gets the internal netty channel.
		 *
		 * @return The netty channel.
		 */
		public Channel getChannel() {
			return this.event.getChannel();
		}

		/**
		 * Sets the internal netty channel.
		 *
		 * @param connection The netty channel.
		 */
		public void setConnection(Connection connection) {
			this.event.setConnection(connection);
		}
	}

}
