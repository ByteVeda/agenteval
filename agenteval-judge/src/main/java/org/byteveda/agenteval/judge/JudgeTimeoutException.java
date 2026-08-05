package org.byteveda.agenteval.judge;

import java.time.Duration;

/**
 * Thrown when a judge LLM request exceeds the configured timeout.
 */
public class JudgeTimeoutException extends JudgeException {

    private static final long serialVersionUID = 1L;

    private final Duration timeout;

    public JudgeTimeoutException(String message, Duration timeout) {
        super(message);
        this.timeout = timeout;
    }

    public JudgeTimeoutException(String message, Duration timeout, Throwable cause) {
        super(message, cause);
        this.timeout = timeout;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
