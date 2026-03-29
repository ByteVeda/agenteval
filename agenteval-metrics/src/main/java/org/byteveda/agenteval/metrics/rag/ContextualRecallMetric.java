package org.byteveda.agenteval.metrics.rag;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Measures whether the retrieval context contains the information needed
 * to produce the expected output.
 *
 * <p>Evaluates recall: are all facts in the expected output supported
 * by at least one retrieved document?</p>
 */
public final class ContextualRecallMetric extends LLMJudgeMetric {

    private static final String NAME = "ContextualRecall";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/contextual-recall.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public ContextualRecallMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public ContextualRecallMetric(JudgeModel judge, double threshold) {
        super(judge, threshold, PROMPT_PATH);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void validate(AgentTestCase testCase) {
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
        vars.put("expectedOutput", testCase.getExpectedOutput());
        vars.put("retrievalContext",
                ContextualPrecisionMetric.formatContext(testCase.getRetrievalContext()));
        return vars;
    }
}
