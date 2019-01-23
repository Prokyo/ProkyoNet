package de.prokyo.network.common.exception;

/**
 * Used for any kind of exceptions/errors while decoding data.
 */
public class DecodingException extends RuntimeException {

	/**
	 * Constructor
	 */
	public DecodingException() {
	}

	/**
	 * Constructor<br>
	 *
	 * @param msg The message containing important information for the user
	 */
	public DecodingException(String msg) {
		super(msg);
	}

	/**
	 * Constructor<br>
	 *
	 * @param msg The message containing important information for the user<br>
	 * @param throwable The original throwable instance this exception will be a wrapper for
	 */
	public DecodingException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

}
