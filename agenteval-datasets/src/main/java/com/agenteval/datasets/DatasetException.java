package com.agenteval.datasets;

/**
 * Unchecked exception for dataset loading/writing errors.
 */
public class DatasetException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatasetException(String message) {
        super(message);
    }

    public DatasetException(String message, Throwable cause) {
        super(message, cause);
    }
}
