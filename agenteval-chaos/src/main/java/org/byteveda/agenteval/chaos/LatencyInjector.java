package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;

import java.util.List;
import java.util.Objects;

/**
 * Simulates latency by increasing {@code durationMs} on tool calls.
 *
 * <p>Adds the configured additional milliseconds to each tool call's
 * existing duration, simulating slow or degraded tool responses.</p>
 */
public final class LatencyInjector implements ChaosInjector {

    private final long additionalMs;

    /**
     * Creates a latency injector with the specified additional delay.
     *
     * @param additionalMs milliseconds to add to each tool call duration
     * @throws IllegalArgumentException if additionalMs is negative
     */
    public LatencyInjector(long additionalMs) {
        if (additionalMs < 0) {
            throw new IllegalArgumentException(
                    "additionalMs must not be negative, got: " + additionalMs);
        }
        this.additionalMs = additionalMs;
    }

    @Override
    public AgentTestCase inject(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");
        List<ToolCall> originalCalls = testCase.getToolCalls();
        if (originalCalls.isEmpty()) {
            return testCase;
        }

        List<ToolCall> slowCalls = originalCalls.stream()
                .map(tc -> new ToolCall(tc.name(), tc.arguments(),
                        tc.result(), tc.durationMs() + additionalMs))
                .toList();

        return testCase.toBuilder()
                .toolCalls(slowCalls)
                .build();
    }

    @Override
    public String description() {
        return "Adds " + additionalMs + "ms latency to all tool calls";
    }

    /**
     * Returns the additional latency in milliseconds.
     */
    public long getAdditionalMs() {
        return additionalMs;
    }
}
