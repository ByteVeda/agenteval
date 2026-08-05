package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ReasoningStep;
import org.byteveda.agenteval.core.model.ReasoningStepType;
import org.byteveda.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Evaluates the quality of an agent's planning steps.
 *
 * <p>Examines the PLAN-type reasoning steps for clarity, feasibility,
 * completeness, and logical ordering.</p>
 */
public final class PlanQualityMetric extends LLMJudgeMetric {

    private static final String NAME = "PlanQuality";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/plan-quality.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public PlanQualityMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public PlanQualityMetric(JudgeModel judge, double threshold) {
        super(judge, threshold, PROMPT_PATH);
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
        return vars;
    }

    private String formatPlanSteps(AgentTestCase testCase) {
        var planSteps = testCase.getReasoningTrace().stream()
                .filter(step -> step.type() == ReasoningStepType.PLAN)
                .toList();
        if (planSteps.isEmpty()) {
            return testCase.getReasoningTrace().stream()
                    .map(step -> "[" + step.type() + "] " + step.content())
                    .collect(Collectors.joining("\n"));
        }
        return planSteps.stream()
                .map(ReasoningStep::content)
                .collect(Collectors.joining("\n"));
    }
}
