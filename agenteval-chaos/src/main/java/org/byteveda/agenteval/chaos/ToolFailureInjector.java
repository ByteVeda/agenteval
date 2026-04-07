package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;

import java.util.List;
import java.util.Objects;

/**
 * Replaces tool call results with error messages to simulate tool failures.
 *
 * <p>Creates a new test case via {@code toBuilder()} with modified tool calls
 * where results are replaced with error strings.</p>
 */
public final class ToolFailureInjector implements ChaosInjector {

    private static final List<String> ERROR_MESSAGES = List.of(
            "ERROR: Tool unavailable",
            "ERROR: Connection timeout",
            "ERROR: Service returned 500 Internal Server Error",
            "ERROR: Rate limit exceeded",
            "ERROR: Authentication failed"
    );

    private final String errorMessage;

    /**
     * Creates an injector that replaces all tool results with the given error.
     *
     * @param errorMessage the error message to inject
     */
    public ToolFailureInjector(String errorMessage) {
        this.errorMessage = Objects.requireNonNull(errorMessage,
                "errorMessage must not be null");
    }

    /**
     * Creates an injector that replaces all tool results with
     * "ERROR: Tool unavailable".
     */
    public ToolFailureInjector() {
        this(ERROR_MESSAGES.getFirst());
    }

    @Override
    public AgentTestCase inject(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");
        List<ToolCall> originalCalls = testCase.getToolCalls();
        if (originalCalls.isEmpty()) {
            return testCase;
        }

        List<ToolCall> failedCalls = originalCalls.stream()
                .map(tc -> new ToolCall(tc.name(), tc.arguments(),
                        errorMessage, tc.durationMs()))
                .toList();

        return testCase.toBuilder()
                .toolCalls(failedCalls)
                .build();
    }

    @Override
    public String description() {
        return "Replaces tool call results with: " + errorMessage;
    }

    /**
     * Returns the list of built-in error messages.
     */
    public static List<String> defaultErrorMessages() {
        return ERROR_MESSAGES;
    }
}
