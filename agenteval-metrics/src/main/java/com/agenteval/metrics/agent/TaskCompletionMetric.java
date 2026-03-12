package com.agenteval.metrics.agent;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Evaluates whether the agent successfully completed the assigned task.
 *
 * <p>LLM-as-judge determines if the final outcome satisfies the original task,
 * with optional custom success criteria.</p>
 */
public final class TaskCompletionMetric extends LLMJudgeMetric {

    private static final String NAME = "TaskCompletion";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/task-completion.txt";
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final String successCriteria;

    public TaskCompletionMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, null);
    }

    public TaskCompletionMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, null);
    }

    public TaskCompletionMetric(JudgeModel judge, double threshold, String successCriteria) {
        super(judge, threshold, PROMPT_PATH);
        this.successCriteria = successCriteria;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("input", testCase.getInput());
        vars.put("actualOutput", testCase.getActualOutput());
        vars.put("successCriteria", successCriteria != null
                ? "Success Criteria: " + successCriteria
                : "");
        vars.put("toolCalls", formatToolCalls(testCase));
        return vars;
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
