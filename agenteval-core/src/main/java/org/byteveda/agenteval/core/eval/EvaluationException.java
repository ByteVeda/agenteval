package org.byteveda.agenteval.core.eval;

/**
 * Thrown when an evaluation fails due to an unrecoverable error.
 */
public class EvaluationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
