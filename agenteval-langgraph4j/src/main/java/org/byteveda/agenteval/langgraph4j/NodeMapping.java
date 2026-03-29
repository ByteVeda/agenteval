package org.byteveda.agenteval.langgraph4j;

import org.byteveda.agenteval.core.model.ReasoningStepType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configurable mapping from LangGraph4j node names to reasoning step types.
 *
 * <pre>{@code
 * var mapping = NodeMapping.builder()
 *     .map("planner", ReasoningStepType.PLAN)
 *     .map("tool_executor", ReasoningStepType.ACTION)
 *     .map("observer", ReasoningStepType.OBSERVATION)
 *     .build();
 * }</pre>
 */
public final class NodeMapping {

    private final Map<String, ReasoningStepType> mappings;
    private final ReasoningStepType defaultType;

    private NodeMapping(Builder builder) {
        this.mappings = Map.copyOf(builder.mappings);
        this.defaultType = builder.defaultType;
    }

    /**
     * Returns the reasoning step type for the given node name.
     */
    public ReasoningStepType typeFor(String nodeName) {
        return mappings.getOrDefault(nodeName, defaultType);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a default mapping that maps all nodes to THOUGHT.
     */
    public static NodeMapping defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private final Map<String, ReasoningStepType> mappings = new HashMap<>();
        private ReasoningStepType defaultType = ReasoningStepType.THOUGHT;

        private Builder() {}

        public Builder map(String nodeName, ReasoningStepType type) {
            Objects.requireNonNull(nodeName, "nodeName must not be null");
            Objects.requireNonNull(type, "type must not be null");
            this.mappings.put(nodeName, type);
            return this;
        }

        public Builder defaultType(ReasoningStepType type) {
            this.defaultType = Objects.requireNonNull(type, "type must not be null");
            return this;
        }

        public NodeMapping build() {
            return new NodeMapping(this);
        }
    }
}
