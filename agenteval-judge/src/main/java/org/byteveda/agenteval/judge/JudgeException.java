package org.byteveda.agenteval.judge;

/**
 * Base unchecked exception for judge module errors.
 */
public class JudgeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JudgeException(String message) {
        super(message);
    }

    public JudgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
