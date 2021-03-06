package de.prokyo.network.client;

import de.prokyo.network.common.connection.Connection;
import de.prokyo.network.common.event.EventManager;
import de.prokyo.network.common.packet.Packet;
import de.prokyo.network.common.pipeline.ProkyoCompressor;
import de.prokyo.network.common.pipeline.ProkyoDecompressor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import lombok.Getter;

/**
 * Represents a client connection to a remote host.
 */
public class ProkyoClient implements Connection {

	public static final AttributeKey<ProkyoClient> ATTRIBUTE_KEY = AttributeKey.newInstance("prokyoClient");
	@Getter private final EventManager eventManager = new EventManager();
	private Channel channel;
	@Getter private InetSocketAddress remoteHost;
	private EventLoopGroup workerGroup;
	private boolean connected;

	/**
	 * Add the {@link ProkyoCompressor} and the {@link ProkyoDecompressor} to the channel pipeline.
	 */
	public void enableCompression() {
		this.channel.pipeline()
				.addBefore("prokyoEncoder", "prokyoCompressor", new ProkyoCompressor())
				.addBefore("prokyoDecoder", "prokyoDecompressor", new ProkyoDecompressor());
	}

	/**
	 * Removes the {@link ProkyoCompressor} and the {@link ProkyoDecompressor} from the channel pipeline.
	 */
	public void disableCompression() {
		this.channel.pipeline().remove(ProkyoCompressor.class);
		this.channel.pipeline().remove(ProkyoDecompressor.class);
	}

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
		this.remoteHost = new InetSocketAddress(host, port);
		boolean epoll = Epoll.isAvailable();
		this.workerGroup = epoll ? new EpollEventLoopGroup(threads) : new NioEventLoopGroup(threads);

		Bootstrap bootstrap = new Bootstrap()
				.group(this.workerGroup)
				.channel(epoll ? EpollSocketChannel.class : NioSocketChannel.class)
				.handler(new ProkyoClientInitializer(this));

		this.channel = bootstrap.connect(this.remoteHost).sync().channel();
		this.connected = true;
	}

	@Override
	public void sendPacket(Packet packet) {
		this.channel.writeAndFlush(packet);
	}

	/**
	 * Closes the connection synchronously.
	 */
	public void shutdown() {
		if (this.connected) {
			try {
				this.workerGroup.shutdownGracefully().sync();
				this.connected = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Closes the connections asynchronously.
	 */
	public void shutdownAsync() {
		if (this.connected) {
			this.workerGroup.shutdownGracefully();
			this.connected = false;
		}
	}

}
