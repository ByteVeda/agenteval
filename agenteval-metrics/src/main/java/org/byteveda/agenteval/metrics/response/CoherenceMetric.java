package org.byteveda.agenteval.metrics.response;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Measures whether the output is logically coherent and well-structured.
 *
 * <p>Higher score = more coherent (1.0 = perfectly coherent).</p>
 */
public final class CoherenceMetric extends LLMJudgeMetric {

    private static final String NAME = "Coherence";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/coherence.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public CoherenceMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public CoherenceMetric(JudgeModel judge, double threshold) {
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
