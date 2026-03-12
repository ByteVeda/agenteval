package com.agenteval.metrics.agent;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Evaluates whether the agent's execution adhered to its planned steps.
 *
 * <p>Compares PLAN-type reasoning steps against ACTION/OBSERVATION steps
 * to determine if the agent followed its own plan.</p>
 */
public final class PlanAdherenceMetric extends LLMJudgeMetric {

    private static final String NAME = "PlanAdherence";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/plan-adherence.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public PlanAdherenceMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public PlanAdherenceMetric(JudgeModel judge, double threshold) {
        super(judge, threshold, PROMPT_PATH);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void validate(AgentTestCase testCase) {
        super.validate(testCase);
        if (testCase.getReasoningTrace().isEmpty()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty reasoningTrace");
        }
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("input", testCase.getInput());
        vars.put("reasoningTrace", formatPlanSteps(testCase));
        vars.put("toolCalls", formatToolCalls(testCase));
        vars.put("actualOutput", testCase.getActualOutput());
        return vars;
    }

    private String formatPlanSteps(AgentTestCase testCase) {
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
