package com.agenteval.metrics.rag;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Measures whether the retrieved context items are relevant to the expected output.
 *
 * <p>Evaluates if the retrieval context contains information that is
 * useful for producing the expected output, penalizing irrelevant retrievals.</p>
 */
public final class ContextualPrecisionMetric extends LLMJudgeMetric {

    private static final String NAME = "ContextualPrecision";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/contextual-precision.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public ContextualPrecisionMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public ContextualPrecisionMetric(JudgeModel judge, double threshold) {
        super(judge, threshold, PROMPT_PATH);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void validate(AgentTestCase testCase) {
        super.validate(testCase);
        if (testCase.getRetrievalContext().isEmpty()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty retrievalContext");
        }
        if (testCase.getExpectedOutput() == null || testCase.getExpectedOutput().isBlank()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty expectedOutput");
        }
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("input", testCase.getInput());
        vars.put("actualOutput", testCase.getActualOutput());
        vars.put("expectedOutput", testCase.getExpectedOutput());
        vars.put("retrievalContext", formatContext(testCase.getRetrievalContext()));
        return vars;
    }

    static String formatContext(List<String> context) {
        var sb = new StringBuilder();
        for (int i = 0; i < context.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(context.get(i));
            if (i < context.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
