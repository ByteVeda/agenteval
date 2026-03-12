package com.agenteval.metrics.response;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Measures whether the output is relevant to the input question.
 *
 * <p>Uses LLM-as-judge to evaluate how well the output addresses the input,
 * penalizing off-topic content and irrelevant information.</p>
 */
public final class AnswerRelevancyMetric extends LLMJudgeMetric {

    private static final String NAME = "AnswerRelevancy";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/answer-relevancy.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    private final boolean strictMode;

    public AnswerRelevancyMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, false);
    }

    public AnswerRelevancyMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, false);
    }

    public AnswerRelevancyMetric(JudgeModel judge, double threshold, boolean strictMode) {
        super(judge, threshold, PROMPT_PATH);
        this.strictMode = strictMode;
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
        vars.put("strictMode", strictMode
                ? "- In strict mode: any off-topic content should heavily penalize the score"
                : "");
        return vars;
    }
}
