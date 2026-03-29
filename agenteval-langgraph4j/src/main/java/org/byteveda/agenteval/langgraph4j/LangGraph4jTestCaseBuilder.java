package org.byteveda.agenteval.langgraph4j;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ReasoningStep;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts LangGraph4j execution data into an AgentTestCase.
 */
public final class LangGraph4jTestCaseBuilder {

    private LangGraph4jTestCaseBuilder() {}

    /**
     * Builds an AgentTestCase from graph execution results.
     *
     * @param input the original input to the graph
     * @param output the final output from the graph
     * @param steps the reasoning steps captured during execution
     * @param latencyMs the total execution time in milliseconds
     * @return the constructed test case
     */
    public static AgentTestCase build(String input, String output,
                                       List<ReasoningStep> steps, long latencyMs) {
        Objects.requireNonNull(input, "input must not be null");
        var builder = AgentTestCase.builder()
                .input(input)
                .actualOutput(output)
                .reasoningTrace(steps)
                .metadata(Map.of("framework", "langgraph4j"));
        var tc = builder.build();
        tc.setLatencyMs(latencyMs);
        return tc;
    }
}
