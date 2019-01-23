package de.prokyo.network.common.exception;

/**
 * Used for any kind of exceptions/errors while encoding data.
 */
public class EncodingException extends RuntimeException {

	/**
	 * Constructor
	 */
	public EncodingException() {
	}

	/**
	 * Constructor
	 *
	 * @param msg The message containing important information for the user
	 */
	public EncodingException(String msg) {
		super(msg);
	}

	/**
	 * Constructor
	 *
	 * @param msg The message containing important information for the user<br>
	 * @param throwable The original throwable instance this exception will be a wrapper for
	 */
	public EncodingException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

}
