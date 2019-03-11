package de.prokyo.network.common.misc;

public enum ServerType {

	SERVER,
	CLIENT,
	UNKNOWN;

	public static ServerType fromOrdinal(int ordinal) {
		switch (ordinal) {
			case 0:
				return SERVER;
			case 1:
				return CLIENT;
			default:
				return UNKNOWN;
		}
	}
}
