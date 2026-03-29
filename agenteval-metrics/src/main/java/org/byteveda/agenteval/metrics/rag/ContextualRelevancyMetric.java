package org.byteveda.agenteval.metrics.rag;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Measures whether the retrieval context is relevant to the input query.
 *
 * <p>Evaluates if the retrieved documents are topically related to
 * the user's question, regardless of the expected output.</p>
 */
public final class ContextualRelevancyMetric extends LLMJudgeMetric {

    private static final String NAME = "ContextualRelevancy";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/contextual-relevancy.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public ContextualRelevancyMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public ContextualRelevancyMetric(JudgeModel judge, double threshold) {
        super(judge, threshold, PROMPT_PATH);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void validate(AgentTestCase testCase) {
        if (testCase.getInput() == null || testCase.getInput().isBlank()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty input");
        }
        if (testCase.getRetrievalContext().isEmpty()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty retrievalContext");
        }
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("input", testCase.getInput());
        vars.put("retrievalContext",
                ContextualPrecisionMetric.formatContext(testCase.getRetrievalContext()));
        return vars;
    }
}
