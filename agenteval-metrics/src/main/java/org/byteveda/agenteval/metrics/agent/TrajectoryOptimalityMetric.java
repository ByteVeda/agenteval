package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Evaluates whether the agent took the most efficient path to complete its task.
 *
 * <p>Penalizes unnecessary tool calls, redundant LLM invocations, and
 * circular reasoning. When {@code maxSteps} is configured, step counts
 * exceeding the limit are penalized by the judge.</p>
 */
public final class TrajectoryOptimalityMetric extends LLMJudgeMetric {

    private static final String NAME = "TrajectoryOptimality";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/trajectory-optimality.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;
    private static final int DEFAULT_MAX_STEPS = -1;

    private final int maxSteps;

    public TrajectoryOptimalityMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, DEFAULT_MAX_STEPS);
    }

    public TrajectoryOptimalityMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, DEFAULT_MAX_STEPS);
    }

    public TrajectoryOptimalityMetric(JudgeModel judge, double threshold, int maxSteps) {
        super(judge, threshold, PROMPT_PATH);
        this.maxSteps = maxSteps;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void validate(AgentTestCase testCase) {
        if (testCase.getInput() == null || testCase.getInput().isBlank()) {
            throw new IllegalArgumentException(NAME + " requires non-empty input");
        }
        if (testCase.getReasoningTrace().isEmpty() && testCase.getToolCalls().isEmpty()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty reasoningTrace or toolCalls");
        }
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("input", testCase.getInput());
        vars.put("actualOutput", testCase.getActualOutput() != null
                ? testCase.getActualOutput() : "(no output)");
        vars.put("reasoningTrace", formatReasoningTrace(testCase));
        vars.put("toolCalls", formatToolCalls(testCase));
        vars.put("totalSteps", String.valueOf(testCase.getReasoningTrace().size()));
        vars.put("totalToolCalls", String.valueOf(testCase.getToolCalls().size()));
        vars.put("maxSteps", maxSteps > 0 ? String.valueOf(maxSteps) : "(not set)");
        return vars;
    }

    private String formatReasoningTrace(AgentTestCase testCase) {
        if (testCase.getReasoningTrace().isEmpty()) {
            return "(none)";
        }
        return testCase.getReasoningTrace().stream()
                .map(step -> "[" + step.type() + "] " + step.content())
                .collect(Collectors.joining("\n"));
    }

    private String formatToolCalls(AgentTestCase testCase) {
        if (testCase.getToolCalls().isEmpty()) {
            return "(none)";
        }
        return testCase.getToolCalls().stream()
                .map(tc -> tc.name() + "(" + tc.arguments() + ")")
                .collect(Collectors.joining(", "));
    }
}
