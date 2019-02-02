package de.prokyo.network;

import de.prokyo.network.client.ProkyoClient;
import de.prokyo.network.common.compression.CompressionUtil;
import de.prokyo.network.common.event.EventHandler;
import de.prokyo.network.common.packet.PacketRegistry;
import de.prokyo.network.server.ClientConnection;
import de.prokyo.network.server.ProkyoServer;
import de.prokyo.network.server.event.ConnectionEstablishedEvent;
import de.prokyo.network.server.event.ServerStartEvent;
import java.util.Random;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Unit test to test the connection between the client and the server.
 */
public class CompressedConnectionTest {

	private static final byte[] DATA = new byte[512];

	static boolean serverReceived = false;
	static boolean clientReceived = false;

	static {
		Random random = new Random();

		for (int n = 0; n < DATA.length; n++) {
			DATA[n] = (byte) random.nextInt();
		}
	}

	private ProkyoServer server;
	private ProkyoClient client;
	private ClientConnection clientConnection;
	private ExecutorService executor = Executors.newFixedThreadPool(2);

	private final Object lock = new Object();

	/**
	 * Starts a server and a client, enables compression and sends a packet from the client to the server. If the packet is successfully decoded from the server the test passes.
	 *
	 */
	@Ignore("Takes to long. Can be started manually.")
	@Test
	@SneakyThrows
	public void testConnection() {
		PacketRegistry.INSTANCE.register(CompressionPingPacket.class, 0x01);
		CompressionUtil.init(CompressionUtil.CompressionType.LZ4_FASTEST);

		this.server = new ProkyoServer();
		this.client = new ProkyoClient();

		EventHandler<ServerStartEvent> serverStartHandler = this::onServerStart;
		EventHandler<ConnectionEstablishedEvent> connectionEstablishedEventEventHandler = this::onServerConnectionEstablished;
		EventHandler<CompressionPingPacket> serverPingPacketHandler = this::onClientPing;
		this.server.getEventManager().register(ServerStartEvent.class, serverStartHandler);
		this.server.getEventManager().register(ConnectionEstablishedEvent.class, connectionEstablishedEventEventHandler);
		this.server.getEventManager().register(CompressionPingPacket.class, serverPingPacketHandler);

		EventHandler<CompressionPingPacket> clientPingPacketHandler = this::onServerPing;
		EventHandler<de.prokyo.network.client.event.ConnectionEstablishedEvent> clientConnectionEstablishedEventHandler = this::onClientConnectionEstablished;
		this.client.getEventManager().register(CompressionPingPacket.class, clientPingPacketHandler);
		this.client.getEventManager().register(de.prokyo.network.client.event.ConnectionEstablishedEvent.class,
				clientConnectionEstablishedEventHandler);

		executor.execute(() -> {
			try {
				server.start("127.0.0.1", 1337, 1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		synchronized (this.lock) {
			try {
				this.lock.wait(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Assert.assertTrue(serverReceived);
		Assert.assertTrue(clientReceived);
	}

	public void onServerStart(ServerStartEvent event) {
		executor.execute(() -> {
			try {
				client.connect("127.0.0.1", 1337, 1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	public void onServerConnectionEstablished(ConnectionEstablishedEvent event) {
		this.clientConnection = event.getClientConnection();
		this.clientConnection.enableCompression();
		this.client.sendPacket(new CompressionPingPacket(CompressionPingPacket.Sender.CLIENT, DATA));
	}

	public void onClientConnectionEstablished(de.prokyo.network.client.event.ConnectionEstablishedEvent event) {
		event.getProkyoClient().enableCompression();
	}

	public void onClientPing(CompressionPingPacket packet) {
		this.clientConnection.sendPacket(new CompressionPingPacket(CompressionPingPacket.Sender.SERVER, DATA));
	}

	public void onServerPing(CompressionPingPacket packet) {
		Assert.assertArrayEquals(DATA, packet.getData());
		synchronized (this.lock) {
			this.lock.notifyAll();
		}
	}

}
