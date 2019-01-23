package de.prokyo.network.common.packet;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The packet registry handles all kinds of mapping between the java class (via reflections) and the packet ids.
 */
public class PacketRegistry {

	@Getter private static final PacketRegistry instance = new PacketRegistry();

	private final Map<Class<? extends Packet>, Integer> classToPacketId;
	private final Map<Integer, Class<? extends Packet>> packetIdToClass;

	/**
	 * Constructor
	 *
	 * The maps will be instantiated with concurrent implementations.
	 */
	public PacketRegistry() {
		this(true);
	}

	/**
	 * Constructor
	 *
	 * @param concurrentMaps Whether concurrent maps shall be used
	 */
	public PacketRegistry(boolean concurrentMaps) {
		this.classToPacketId = concurrentMaps ? new ConcurrentHashMap<>() : new HashMap<>();
		this.packetIdToClass = concurrentMaps ? new ConcurrentHashMap<>() : new HashMap<>();
	}

	/**
	 * Registers the given packet including it's class and packet id.
	 *
	 * @param clazz The class of the packet
	 * @param packetId The packet id
	 */
	public void register(Class<? extends Packet> clazz, Integer packetId) {
		if(packetId < 0) throw new IllegalArgumentException("The packet's id cannot be lower than zero.");
		if(clazz == null) throw new IllegalArgumentException("The class cannot be null");

		this.classToPacketId.put(clazz, packetId);
		this.packetIdToClass.put(packetId, clazz);
	}

	/**
	 * Unregisters the given packet via it's class.
	 *
	 * @param clazz The class of the packet
	 */
	public void unregister(Class<? extends Packet> clazz) {
		if(clazz == null) throw new IllegalArgumentException("The class cannot be null");
		this.packetIdToClass.remove(this.classToPacketId.remove(clazz));
	}

	/**
	 * Unregisters the given packet via it's packet id.
	 *
	 * @param packetId The packet id
	 */
	public void unregister(Integer packetId) {
		if(packetId < 0) throw new IllegalArgumentException("The packet's id cannot be lower than zero.");
		this.classToPacketId.remove(this.packetIdToClass.remove(packetId));
	}

	/**
	 * Gets the corresponding packet id of the given class. <br>
	 * If the given class is unregistered, the error code -404 will be returned.
	 *
	 * @param clazz The corresponding packet class of the wanted packet id
	 * @return The packet id (id >= 0) or an error code (-404 = not found).
	 */
	public int getPacketId(Class<? extends Packet> clazz) {
		Integer packetId = this.classToPacketId.get(clazz);
		if(packetId != null) return packetId;
		return -404;
	}

	/**
	 * Gets the corresponding packet class of the given packet id. <br>
	 * If the given packet id is unregistered/unknown, <i>null</i> will be returned.
	 *
	 * @param packetId The corresponding packet id of the wanted class
	 * @return The packet class or null if there is no class for this id.
	 */
	public Class<? extends Packet> getPacketClass(int packetId) {
		return this.packetIdToClass.get(packetId);
	}

	/**
	 * Creates a new instance of the corresponding class.
	 *
	 * @param packetId The packet id
	 * @param <T> The type of the packet
	 * @return A new instance of <i>T</i>.
	 * @throws InstantiationException If the class is abstract, an interface or has no (visible) zero args constructor.
	 * @throws IllegalAccessException If the class or it's zero args constructor is not accessible.
	 */
	public <T> T newInstance(int packetId) throws InstantiationException, IllegalAccessException {
		return (T) this.newInstance(this.getPacketClass(packetId));
	}

	/**
	 * Creates a new instance of the given class.
	 *
	 * @param clazz The packet class
	 * @param <T> The type of the packet defined by the given class
	 * @return A new instance of <i>T</i>.
	 * @throws InstantiationException If the class is abstract, an interface or has no (visible) zero args constructor.
	 * @throws IllegalAccessException If the class or it's zero args constructor is not accessible.
	 */
	public <T> T newInstance(Class<T> clazz) throws IllegalAccessException, InstantiationException {
		return clazz.newInstance();
	}

}