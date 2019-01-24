package de.prokyo.network.client;

import de.prokyo.network.common.connection.Connection;
import de.prokyo.network.common.packet.Packet;
import de.prokyo.network.common.pipeline.PacketDecoder;
import de.prokyo.network.common.pipeline.PacketEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

/**
 * Represents a client connection to a remote host.
 */
public class ProkyoClient implements Connection {

	private Channel channel;
	private static final AttributeKey<ProkyoClient> ATTRIBUTE_KEY = AttributeKey.newInstance("prokyoClient");

	/**
	 * Connects to the given host and port with given amount of threads.<br>
	 * This method <b>will block</b> the current thread.<br>
	 * The amount of threads will evaluated by netty.
	 *
	 * @param host The remote host
	 * @param port The port of the remote server
	 * @throws InterruptedException If the thread is interrupted by another thread.
	 */
	public void connect(String host, int port) throws InterruptedException {
		this.connect(host, port, 0);
	}

	/**
	 * Connects to the given host and port with given amount of threads.<br>
	 * This method <b>will block</b> the current thread.
	 *
	 * @param host The remote host
	 * @param port The port of the remote server
	 * @param threads The amount of threads netty should use
	 * @throws InterruptedException If the thread is interrupted by another thread.
	 */
	public void connect(String host, int port, int threads) throws InterruptedException {
		boolean epoll = Epoll.isAvailable();
		EventLoopGroup group = epoll ? new EpollEventLoopGroup(threads) : new NioEventLoopGroup(threads);

		Bootstrap bootstrap = new Bootstrap()
				.group(group)
				.channel(epoll ? EpollSocketChannel.class :  NioSocketChannel.class);

		this.channel.pipeline()
				.addFirst("prokyoEncoder", new PacketEncoder())
				.addFirst("prokyoDecoder", new PacketDecoder());

		this.channel = bootstrap.connect(host, port).sync().channel();

		this.channel.attr(ProkyoClient.ATTRIBUTE_KEY);
	}

	@Override
	public void sendPacket(Packet packet) {
		this.channel.writeAndFlush(packet);
	}

}
