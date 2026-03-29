package org.byteveda.agenteval.metrics.response;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * General-purpose G-Eval metric that evaluates correctness using custom criteria.
 *
 * <p>Compares actual output against expected output using LLM-as-judge with
 * chain-of-thought evaluation steps. The most flexible metric — can be configured
 * for any evaluation dimension.</p>
 */
public final class CorrectnessMetric extends LLMJudgeMetric {

    private static final String NAME = "Correctness";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/correctness.txt";
    private static final double DEFAULT_THRESHOLD = 0.5;
    private static final String DEFAULT_CRITERIA =
            "Evaluate whether the actual output correctly answers the question "
                    + "compared to the expected output.";

    private final String criteria;
    private final List<String> steps;

    public CorrectnessMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, DEFAULT_CRITERIA, List.of());
    }

    public CorrectnessMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, DEFAULT_CRITERIA, List.of());
    }

    public CorrectnessMetric(JudgeModel judge, double threshold,
                             String criteria, List<String> steps) {
        super(judge, threshold, PROMPT_PATH);
        this.criteria = criteria != null ? criteria : DEFAULT_CRITERIA;
        this.steps = steps != null ? List.copyOf(steps) : List.of();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void validate(AgentTestCase testCase) {
        super.validate(testCase);
        if (testCase.getExpectedOutput() == null || testCase.getExpectedOutput().isBlank()) {
            throw new IllegalArgumentException(NAME + " requires non-empty expectedOutput");
        }
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("input", testCase.getInput());
        vars.put("actualOutput", testCase.getActualOutput());
        vars.put("expectedOutput", testCase.getExpectedOutput());
        vars.put("criteria", criteria);
        vars.put("steps", formatSteps());
        return vars;
    }

    private String formatSteps() {
        if (steps.isEmpty()) {
            return "1. Compare the actual output to the expected output\n"
                    + "2. Check for factual accuracy\n"
                    + "3. Evaluate completeness of the answer";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            sb.append(i + 1).append(". ").append(steps.get(i));
            if (i < steps.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
