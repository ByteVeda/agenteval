package com.agenteval.metrics.response;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Measures whether claims in the output are supported by the retrieval context.
 *
 * <p>Core metric for RAG pipelines. Extracts individual claims from the output
 * and verifies each against the provided retrieval context.</p>
 */
public final class FaithfulnessMetric extends LLMJudgeMetric {

    private static final String NAME = "Faithfulness";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/faithfulness.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public FaithfulnessMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public FaithfulnessMetric(JudgeModel judge, double threshold) {
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
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("input", testCase.getInput());
        vars.put("actualOutput", testCase.getActualOutput());
        vars.put("retrievalContext", String.join("\n---\n", testCase.getRetrievalContext()));
        return vars;
    }
}
