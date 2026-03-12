package com.agenteval.mcp;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.ToolCall;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts captured MCP data into an AgentTestCase.
 */
public final class McpTestCaseBuilder {

    private McpTestCaseBuilder() {}

    /**
     * Builds an AgentTestCase from MCP capture data.
     *
     * @param input the user input
     * @param output the agent output
     * @param toolCalls the captured MCP tool calls
     * @return the constructed test case
     */
    public static AgentTestCase build(String input, String output,
                                       List<ToolCall> toolCalls) {
        Objects.requireNonNull(input, "input must not be null");
        return AgentTestCase.builder()
                .input(input)
                .actualOutput(output)
                .toolCalls(toolCalls)
                .metadata(Map.of("framework", "mcp"))
                .build();
    }

    /**
     * Builds an AgentTestCase directly from an McpCapture.
     */
    public static AgentTestCase fromCapture(String input, String output,
                                             McpCapture capture) {
        Objects.requireNonNull(capture, "capture must not be null");
        return build(input, output, capture.getCapturedCalls());
    }
}
