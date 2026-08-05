package org.byteveda.agenteval.datasets.generation;

/**
 * Thrown when synthetic dataset generation fails.
 */
public class GenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
