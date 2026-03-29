package org.byteveda.agenteval.core.model;

import java.util.Objects;

/**
 * A single step in an agent's reasoning trace.
 */
public record ReasoningStep(
        ReasoningStepType type,
        String content,
        ToolCall toolCall
) {
    public ReasoningStep {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }

    /**
     * Creates a reasoning step without an associated tool call.
     */
    public static ReasoningStep of(ReasoningStepType type, String content) {
        return new ReasoningStep(type, content, null);
    }

    /**
     * Creates an ACTION reasoning step with an associated tool call.
     */
    public static ReasoningStep action(String content, ToolCall toolCall) {
        return new ReasoningStep(ReasoningStepType.ACTION, content, toolCall);
    }
}
