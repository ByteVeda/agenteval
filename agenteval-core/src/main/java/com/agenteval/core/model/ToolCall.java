package com.agenteval.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a tool invocation made by an agent.
 */
public record ToolCall(
        String name,
        Map<String, Object> arguments,
        String result,
        long durationMs
) {
    public ToolCall {
        Objects.requireNonNull(name, "name must not be null");
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    /**
     * Creates a ToolCall with just a name (minimal form).
     */
    public static ToolCall of(String name) {
        return new ToolCall(name, Map.of(), null, 0);
    }

    /**
     * Creates a ToolCall with name and arguments.
     */
    public static ToolCall of(String name, Map<String, Object> arguments) {
        return new ToolCall(name, arguments, null, 0);
    }
}
