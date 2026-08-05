package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;

import java.util.List;
import java.util.Objects;

/**
 * Modifies tool call result strings to simulate schema changes.
 *
 * <p>Wraps tool results in unexpected JSON structures, simulating
 * API version changes or schema mutations that agents must handle
 * gracefully.</p>
 */
public final class SchemaMutationInjector implements ChaosInjector {

    private final MutationType mutationType;

    /**
     * Types of schema mutation that can be applied.
     */
    public enum MutationType {
        /** Wraps the result in an unexpected JSON envelope. */
        WRAP_IN_ENVELOPE,
        /** Replaces the result with a partial/truncated version. */
        TRUNCATE,
        /** Wraps the result in a nested "data" field. */
        NEST_IN_DATA
    }

    /**
     * Creates an injector with the specified mutation type.
     *
     * @param mutationType the type of schema mutation
     */
    public SchemaMutationInjector(MutationType mutationType) {
        this.mutationType = Objects.requireNonNull(mutationType,
                "mutationType must not be null");
    }

    /**
     * Creates an injector with {@link MutationType#WRAP_IN_ENVELOPE} by default.
     */
    public SchemaMutationInjector() {
        this(MutationType.WRAP_IN_ENVELOPE);
    }

    @Override
    public AgentTestCase inject(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");
        List<ToolCall> originalCalls = testCase.getToolCalls();
        if (originalCalls.isEmpty()) {
            return testCase;
        }

        List<ToolCall> mutatedCalls = originalCalls.stream()
                .map(tc -> new ToolCall(tc.name(), tc.arguments(),
                        mutateResult(tc.result()), tc.durationMs()))
                .toList();

        return testCase.toBuilder()
                .toolCalls(mutatedCalls)
                .build();
    }

    @Override
    public String description() {
        return "Mutates tool result schemas using: " + mutationType;
    }

    /**
     * Returns the mutation type used by this injector.
     */
    public MutationType getMutationType() {
        return mutationType;
    }

    private String mutateResult(String result) {
        if (result == null) {
            return "{\"error\": \"unexpected_schema\", \"version\": \"2.0\"}";
        }

        return switch (mutationType) {
            case WRAP_IN_ENVELOPE ->
                    "{\"status\": \"ok\", \"version\": \"2.0\", "
                            + "\"payload\": " + escapeForJson(result) + "}";
            case TRUNCATE -> truncateResult(result);
            case NEST_IN_DATA ->
                    "{\"data\": {\"result\": " + escapeForJson(result)
                            + ", \"metadata\": {\"deprecated\": true}}}";
        };
    }

    private static String truncateResult(String result) {
        int len = result.length();
        if (len <= 10) {
            return result.substring(0, Math.max(1, len / 2)) + "...";
        }
        return result.substring(0, len / 2) + "... [TRUNCATED]";
    }

    private static String escapeForJson(String value) {
        return "\"" + value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
