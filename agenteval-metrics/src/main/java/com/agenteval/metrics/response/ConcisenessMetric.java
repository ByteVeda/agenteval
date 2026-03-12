package com.agenteval.metrics.response;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Measures whether the output is concise and free of unnecessary verbosity.
 *
 * <p>Higher score = more concise (1.0 = optimally concise).</p>
 */
public final class ConcisenessMetric extends LLMJudgeMetric {

    private static final String NAME = "Conciseness";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/conciseness.txt";
    private static final double DEFAULT_THRESHOLD = 0.5;

    public ConcisenessMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public ConcisenessMetric(JudgeModel judge, double threshold) {
        super(judge, threshold, PROMPT_PATH);
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
        return vars;
    }
}
