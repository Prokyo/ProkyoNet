package de.prokyo.network;

import de.prokyo.network.client.ProkyoClient;
import de.prokyo.network.server.ClientConnection;
import de.prokyo.network.server.event.ConnectionEstablishedEvent;
import de.prokyo.network.common.event.EventHandler;
import de.prokyo.network.common.packet.PacketRegistry;
import de.prokyo.network.server.ProkyoServer;
import de.prokyo.network.server.event.ServerStartEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test to test the connection between the client and the server.
 */
public class ConnectionTest {

	static boolean serverReceived = false;
	static boolean clientReceived = false;

	private ProkyoServer server;
	private ProkyoClient client;
	private ClientConnection clientConnection;
	private ExecutorService executor = Executors.newFixedThreadPool(2);

	private final Object lock = new Object();

	private long time = System.currentTimeMillis();

	/**
	 * Starts a server and a client and sends a packet from the client to the server. If the packet is successfully decoded from the server the test passes.
	 *
	 */
	@Ignore("Takes to long. Can be started manually.")
	@Test
	public void testConnection() {
		PacketRegistry.INSTANCE.register(PingPacket.class, 0x01);

		this.server = new ProkyoServer();
		this.client = new ProkyoClient();

		EventHandler<ServerStartEvent> serverStartHandler = this::onServerStart;
		EventHandler<ConnectionEstablishedEvent> connectionEstablishedEventEventHandler = this::onServerConnectionEstablished;
		EventHandler<PingPacket> serverPingPacketHandler = this::onClientPing;
		this.server.getEventManager().register(ServerStartEvent.class, serverStartHandler);
		this.server.getEventManager().register(ConnectionEstablishedEvent.class, connectionEstablishedEventEventHandler);
		this.server.getEventManager().register(PingPacket.class, serverPingPacketHandler);

		EventHandler<PingPacket> clientPingPacketHandler = this::onServerPing;
		this.client.getEventManager().register(PingPacket.class, clientPingPacketHandler);

		executor.execute(() -> {
			try {
				server.start("127.0.0.1", 1337, 1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		synchronized (this.lock) {
			try {
				this.lock.wait(2000);
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
		this.client.sendPacket(new PingPacket(PingPacket.Sender.CLIENT, this.time));
	}

	public void onClientPing(PingPacket packet) {
		this.clientConnection.sendPacket(new PingPacket(PingPacket.Sender.SERVER, this.time));
	}

	public void onServerPing(PingPacket packet) {
		Assert.assertEquals(this.time, packet.getTime());
		synchronized (this.lock) {
			this.lock.notifyAll();
		}
	}

}
