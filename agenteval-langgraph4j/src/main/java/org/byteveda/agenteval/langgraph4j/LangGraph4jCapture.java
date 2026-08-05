package org.byteveda.agenteval.langgraph4j;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ReasoningStep;
import org.bsc.langgraph4j.NodeOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Captures LangGraph4j graph execution results as AgentTestCase instances.
 *
 * <p>Wraps graph execution, maps node outputs to reasoning steps using
 * a configurable {@link NodeMapping}.</p>
 *
 * <pre>{@code
 * var capture = new LangGraph4jCapture(nodeMapping);
 * AgentTestCase testCase = capture.fromNodeOutputs("input", nodeOutputs);
 * }</pre>
 */
public final class LangGraph4jCapture {

    private static final Logger LOG = LoggerFactory.getLogger(LangGraph4jCapture.class);

    private final NodeMapping nodeMapping;

    public LangGraph4jCapture() {
        this(NodeMapping.defaults());
    }

    public LangGraph4jCapture(NodeMapping nodeMapping) {
        this.nodeMapping = Objects.requireNonNull(nodeMapping,
                "nodeMapping must not be null");
    }

    /**
     * Converts a list of LangGraph4j node outputs into an AgentTestCase.
     *
     * @param input the original input to the graph
     * @param nodeOutputs the ordered list of node outputs from graph execution
     * @return the captured test case with reasoning trace
     */
    public AgentTestCase fromNodeOutputs(String input,
                                          List<NodeOutput<? extends Map<String, Object>>> nodeOutputs) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(nodeOutputs, "nodeOutputs must not be null");

        LOG.debug("Capturing LangGraph4j execution: {} nodes for input: {}",
                nodeOutputs.size(),
                input.length() > 100 ? input.substring(0, 100) + "..." : input);

        long startTime = System.currentTimeMillis();
        List<ReasoningStep> steps = new ArrayList<>();
        String lastOutput = null;

        for (NodeOutput<? extends Map<String, Object>> nodeOutput : nodeOutputs) {
            String nodeName = nodeOutput.node();
            var stepType = nodeMapping.typeFor(nodeName);
            String content = formatNodeState(nodeOutput.state());

            steps.add(ReasoningStep.of(stepType, nodeName + ": " + content));
            lastOutput = content;
        }

        long latencyMs = System.currentTimeMillis() - startTime;
        return LangGraph4jTestCaseBuilder.build(input, lastOutput, steps, latencyMs);
    }

    private static String formatNodeState(Map<String, Object> state) {
        if (state == null || state.isEmpty()) return "(empty state)";
        var sb = new StringBuilder();
        state.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(k).append("=").append(v);
        });
        return sb.toString();
    }
}
