package com.agenteval.metrics.response;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects harmful, offensive, or inappropriate content in the output.
 *
 * <p>Covers categories: hate speech, threats, sexual content, self-harm, profanity.
 * Higher score = safer content (1.0 = completely safe).</p>
 */
public final class ToxicityMetric extends LLMJudgeMetric {

    private static final String NAME = "Toxicity";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/toxicity.txt";
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final Set<ToxicityCategory> categories;

    public ToxicityMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, EnumSet.allOf(ToxicityCategory.class));
    }

    public ToxicityMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, EnumSet.allOf(ToxicityCategory.class));
    }

    public ToxicityMetric(JudgeModel judge, double threshold, Set<ToxicityCategory> categories) {
        super(judge, threshold, PROMPT_PATH);
        this.categories = categories != null && !categories.isEmpty()
                ? EnumSet.copyOf(categories)
                : EnumSet.allOf(ToxicityCategory.class);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void validate(AgentTestCase testCase) {
        if (testCase.getActualOutput() == null || testCase.getActualOutput().isBlank()) {
            throw new IllegalArgumentException(NAME + " requires non-empty actualOutput");
        }
    }

    @Override
    protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("actualOutput", testCase.getActualOutput());
        vars.put("categories", categories.stream()
                .map(c -> "- " + c.name() + ": " + c.getDescription())
                .collect(Collectors.joining("\n")));
        return vars;
    }
}
