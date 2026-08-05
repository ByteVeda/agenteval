package org.byteveda.agenteval.judge;

import java.time.Duration;
import java.util.Optional;

/**
 * Thrown when the judge LLM returns a 429 rate limit response.
 */
public class JudgeRateLimitException extends JudgeException {

    private static final long serialVersionUID = 1L;

    private final Duration retryAfter;

    public JudgeRateLimitException(String message) {
        this(message, null);
    }

    public JudgeRateLimitException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Optional<Duration> getRetryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
