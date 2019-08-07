package de.prokyo.network;

import de.prokyo.network.client.ProkyoClient;
import de.prokyo.network.client.event.ConnectionClosedEvent;
import de.prokyo.network.client.event.ConnectionEstablishedEvent;
import de.prokyo.network.common.event.KeepAliveEvent;
import de.prokyo.network.server.ClientConnection;
import de.prokyo.network.server.ProkyoServer;
import de.prokyo.network.server.event.ServerStartEvent;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeepAliveTest {

	private ProkyoServer server;
	private ProkyoClient client;
	private ClientConnection clientConnection;
	private ExecutorService executor = Executors.newFixedThreadPool(2);

	private final Object lock = new Object();

	private boolean success;

	/**
	 * Starts a server and a client, waits about 3 secs and tries to send a KeepAlivePacket. If the packet is received successfully, this test passes.
	 */
	@Ignore("Takes too long. Can be started manually.")
	@Test
	@SneakyThrows
	public void testKeepAlive() {
		try {
			this.server = new ProkyoServer();
			this.server.getEventManager().register(ServerStartEvent.class, this::onServerStart);
			this.server.getEventManager().register(KeepAliveEvent.class, this::onKeepAlive);

			this.client = new ProkyoClient();
			this.client.getEventManager().register(ConnectionClosedEvent.class, this::onClientConnectionClosed);
			this.client.getEventManager().register(KeepAliveEvent.class, this::onKeepAlive);

			this.executor.execute(() -> {
				try {
					this.server.start("localhost", 1337);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			});

			synchronized (this.lock) {
				this.lock.wait(15 * 15_000);
			}

			Assert.assertTrue(this.success);
		} finally {
			this.client.shutdownAsync();
			this.server.shutdownAsync();
		}
	}

	public void onServerStart(ServerStartEvent event) {
		this.executor.execute(() -> {
			try {
				this.client.connect("localhost", 1337);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		});
	}

	public void onKeepAlive(KeepAliveEvent event) {
		this.success = true;
		synchronized (this.lock) {
			this.lock.notifyAll();
		}
	}

	public void onClientConnectionClosed(ConnectionClosedEvent event) {
		this.success = false;
	}

}
