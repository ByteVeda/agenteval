package org.byteveda.agenteval.replay;

/**
 * Thrown when a replay interaction does not match the expected recorded data.
 */
public class ReplayMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReplayMismatchException(String message) {
        super(message);
    }

    public ReplayMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
