package org.byteveda.agenteval.metrics.response;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.HashMap;
import java.util.Map;

/**
 * Measures whether the output contains fabricated information.
 *
 * <p>Specifically targets invented entities, false statistics, and non-existent citations.
 * Higher score = fewer hallucinations (1.0 = no hallucinations).</p>
 */
public final class HallucinationMetric extends LLMJudgeMetric {

    private static final String NAME = "Hallucination";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/hallucination.txt";
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final boolean contextRequired;

    public HallucinationMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, false);
    }

    public HallucinationMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, false);
    }

    public HallucinationMetric(JudgeModel judge, double threshold, boolean contextRequired) {
        super(judge, threshold, PROMPT_PATH);
        this.contextRequired = contextRequired;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void validate(AgentTestCase testCase) {
        super.validate(testCase);
        if (contextRequired && testCase.getContext().isEmpty()
                && testCase.getRetrievalContext().isEmpty()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty context when contextRequired is true");
        }
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("input", testCase.getInput());
        vars.put("actualOutput", testCase.getActualOutput());

        String context;
        if (!testCase.getRetrievalContext().isEmpty()) {
            context = String.join("\n---\n", testCase.getRetrievalContext());
        } else if (!testCase.getContext().isEmpty()) {
            context = String.join("\n---\n", testCase.getContext());
        } else {
            context = "(No context provided — evaluate based on general knowledge)";
        }
        vars.put("context", context);
        return vars;
    }
}
