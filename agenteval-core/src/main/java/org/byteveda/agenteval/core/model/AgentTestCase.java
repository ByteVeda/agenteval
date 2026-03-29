package org.byteveda.agenteval.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single-turn agent evaluation test case.
 *
 * <p>Most fields are immutable and set at construction time via the builder.
 * Capture-time fields ({@code actualOutput}, {@code latencyMs}, {@code tokenUsage},
 * {@code cost}) are mutable to support parameterized dataset tests where the agent
 * response is captured after test case construction.</p>
 */
@JsonDeserialize(builder = AgentTestCase.Builder.class)
public final class AgentTestCase {

    private final String input;
    private volatile String actualOutput;
    private final String expectedOutput;
    private final List<String> retrievalContext;
    private final List<String> context;
    private final List<ToolCall> toolCalls;
    private final List<ToolCall> expectedToolCalls;
    private final List<ReasoningStep> reasoningTrace;
    private volatile long latencyMs;
    private volatile TokenUsage tokenUsage;
    private volatile BigDecimal cost;
    private final Map<String, Object> metadata;

    private AgentTestCase(Builder builder) {
        this.input = Objects.requireNonNull(builder.input, "input must not be null");
        this.actualOutput = builder.actualOutput;
        this.expectedOutput = builder.expectedOutput;
        this.retrievalContext = builder.retrievalContext == null ? List.of() : List.copyOf(builder.retrievalContext);
        this.context = builder.context == null ? List.of() : List.copyOf(builder.context);
        this.toolCalls = builder.toolCalls == null ? List.of() : List.copyOf(builder.toolCalls);
        this.expectedToolCalls = builder.expectedToolCalls == null ? List.of() : List.copyOf(builder.expectedToolCalls);
        this.reasoningTrace = builder.reasoningTrace == null ? List.of() : List.copyOf(builder.reasoningTrace);
        this.latencyMs = builder.latencyMs;
        this.tokenUsage = builder.tokenUsage;
        this.cost = builder.cost;
        this.metadata = builder.metadata == null ? Map.of() : Map.copyOf(builder.metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new builder pre-populated with this test case's values.
     * Useful for creating modified copies (e.g., benchmark variants).
     */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.input = this.input;
        b.actualOutput = this.actualOutput;
        b.expectedOutput = this.expectedOutput;
        b.retrievalContext = this.retrievalContext.isEmpty() ? null : List.copyOf(this.retrievalContext);
        b.context = this.context.isEmpty() ? null : List.copyOf(this.context);
        b.toolCalls = this.toolCalls.isEmpty() ? null : List.copyOf(this.toolCalls);
        b.expectedToolCalls = this.expectedToolCalls.isEmpty() ? null : List.copyOf(this.expectedToolCalls);
        b.reasoningTrace = this.reasoningTrace.isEmpty() ? null : List.copyOf(this.reasoningTrace);
        b.latencyMs = this.latencyMs;
        b.tokenUsage = this.tokenUsage;
        b.cost = this.cost;
        b.metadata = this.metadata.isEmpty() ? null : new java.util.HashMap<>(this.metadata);
        return b;
    }

    // --- Getters ---

    public String getInput() { return input; }
    public String getActualOutput() { return actualOutput; }
    public String getExpectedOutput() { return expectedOutput; }
    public List<String> getRetrievalContext() { return retrievalContext; }
    public List<String> getContext() { return context; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public List<ToolCall> getExpectedToolCalls() { return expectedToolCalls; }
    public List<ReasoningStep> getReasoningTrace() { return reasoningTrace; }
    public long getLatencyMs() { return latencyMs; }
    public TokenUsage getTokenUsage() { return tokenUsage; }
    public BigDecimal getCost() { return cost; }
    public Map<String, Object> getMetadata() { return metadata; }

    // --- Mutable capture-time setters ---

    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public void setTokenUsage(TokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String input;
        private String actualOutput;
        private String expectedOutput;
        private List<String> retrievalContext;
        private List<String> context;
        private List<ToolCall> toolCalls;
        private List<ToolCall> expectedToolCalls;
        private List<ReasoningStep> reasoningTrace;
        private long latencyMs;
        private TokenUsage tokenUsage;
        private BigDecimal cost;
        private Map<String, Object> metadata;

        private Builder() {}

        public Builder input(String input) { this.input = input; return this; }
        public Builder actualOutput(String actualOutput) { this.actualOutput = actualOutput; return this; }
        public Builder expectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; return this; }
        public Builder retrievalContext(List<String> retrievalContext) {
            this.retrievalContext = retrievalContext;
            return this;
        }
        public Builder context(List<String> context) { this.context = context; return this; }
        public Builder toolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; return this; }
        public Builder expectedToolCalls(List<ToolCall> expectedToolCalls) {
            this.expectedToolCalls = expectedToolCalls;
            return this;
        }
        public Builder reasoningTrace(List<ReasoningStep> reasoningTrace) {
            this.reasoningTrace = reasoningTrace;
            return this;
        }
        public Builder latencyMs(long latencyMs) { this.latencyMs = latencyMs; return this; }
        public Builder tokenUsage(TokenUsage tokenUsage) { this.tokenUsage = tokenUsage; return this; }
        public Builder cost(BigDecimal cost) { this.cost = cost; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public AgentTestCase build() {
            return new AgentTestCase(this);
        }
    }
}
