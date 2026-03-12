package com.agenteval.mcp;

import com.agenteval.core.model.ToolCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures MCP tool calls into AgentEval {@link ToolCall} records.
 *
 * <p>Framework-agnostic capture utility. Users convert MCP SDK types
 * into primitive arguments before recording.</p>
 *
 * <pre>{@code
 * var capture = new McpCapture();
 * capture.recordCall("search", Map.of("query", "java"), "results...", 150);
 * List<ToolCall> captured = capture.getCapturedCalls();
 * }</pre>
 */
public final class McpCapture {

    private static final Logger LOG = LoggerFactory.getLogger(McpCapture.class);

    private final List<ToolCall> capturedCalls =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Records an MCP tool call.
     *
     * @param toolName the name of the tool
     * @param arguments the tool arguments
     * @param result the tool result (may be null)
     * @param durationMs the call duration in milliseconds
     */
    public void recordCall(String toolName, Map<String, Object> arguments,
                           String result, long durationMs) {
        LOG.debug("Captured MCP tool call: {} ({}ms)", toolName, durationMs);
        capturedCalls.add(new ToolCall(toolName, arguments, result, durationMs));
    }

    /**
     * Returns all captured tool calls.
     */
    public List<ToolCall> getCapturedCalls() {
        return List.copyOf(capturedCalls);
    }

    /**
     * Clears all captured calls.
     */
    public void clear() {
        capturedCalls.clear();
    }

    /**
     * Converts an arbitrary arguments object to a flat map.
     * Useful for converting MCP SDK argument objects.
     */
    public static Map<String, Object> toArgumentMap(Object mcpArguments) {
        if (mcpArguments == null) return Map.of();
        if (mcpArguments instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Map.of("value", mcpArguments);
    }
}
