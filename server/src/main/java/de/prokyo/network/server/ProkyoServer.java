package de.prokyo.network.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Represents a server handling connections to clients.
 */
public class ProkyoServer {

	/**
	 * Starts a server with the given host address and port with the given amount of threads.<br>
	 * This method <b>will block</b> the current thread.
	 * The amount of threads will be evaluated by netty.
	 *
	 * @param host The remote host
	 * @param port The port of the remote server
	 * @throws InterruptedException If the thread is interrupted by another thread.
	 */
	public void start(String host, int port) throws InterruptedException {
		this.start(host, port, 0);
	}

	/**
	 * Starts a server with the given host address and port with the given amount of threads.<br>
	 * This method <b>will block</b> the current thread.
	 *
	 * @param host    The remote host
	 * @param port    The port of the remote server
	 * @param threads The amount of threads netty should use
	 * @throws InterruptedException If the thread is interrupted by another thread.
	 */
	public void start(String host, int port, int threads) throws InterruptedException {
		boolean epoll = Epoll.isAvailable();
		EventLoopGroup workerGroup = epoll ? new EpollEventLoopGroup(threads) : new NioEventLoopGroup(threads);

		ServerBootstrap serverBootstrap = new ServerBootstrap()
				.group(workerGroup)
				.channel(epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
				.localAddress(host, port);

		serverBootstrap.childHandler(new ClientChannelInitializer()).bind().sync();
	}

}