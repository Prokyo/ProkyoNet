package de.prokyo.network.client;

import de.prokyo.network.common.connection.Connection;
import de.prokyo.network.common.event.EventManager;
import de.prokyo.network.common.packet.Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.Getter;

/**
 * Represents a client connection to a remote host.
 */
public class ProkyoClient implements Connection {

	private Channel channel;
	@Getter private InetSocketAddress remoteHost;
	public static final AttributeKey<ProkyoClient> ATTRIBUTE_KEY = AttributeKey.newInstance("prokyoClient");

	@Getter private final EventManager eventManager = new EventManager();

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
		this.remoteHost = InetSocketAddress.createUnresolved(host, port);
		boolean epoll = Epoll.isAvailable();
		EventLoopGroup group = epoll ? new EpollEventLoopGroup(threads) : new NioEventLoopGroup(threads);

		Bootstrap bootstrap = new Bootstrap()
				.group(group)
				.channel(epoll ? EpollSocketChannel.class :  NioSocketChannel.class)
				.handler(new ProkyoClientInitializer(this));

		this.channel = bootstrap.connect(this.remoteHost).sync().channel();
	}

	@Override
	public void sendPacket(Packet packet) {
		this.channel.writeAndFlush(packet);
	}

}
