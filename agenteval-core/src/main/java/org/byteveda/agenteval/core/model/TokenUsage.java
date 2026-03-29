package org.byteveda.agenteval.core.model;

/**
 * Token usage statistics for an LLM interaction.
 */
public record TokenUsage(int inputTokens, int outputTokens, int totalTokens) {

    public TokenUsage {
        if (inputTokens < 0) throw new IllegalArgumentException("inputTokens must be non-negative");
        if (outputTokens < 0) throw new IllegalArgumentException("outputTokens must be non-negative");
        if (totalTokens < 0) throw new IllegalArgumentException("totalTokens must be non-negative");
    }

    /**
     * Creates a TokenUsage where totalTokens is auto-calculated.
     */
    public static TokenUsage of(int inputTokens, int outputTokens) {
        return new TokenUsage(inputTokens, outputTokens, inputTokens + outputTokens);
    }
}
