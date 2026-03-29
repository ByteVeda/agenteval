package org.byteveda.agenteval.metrics.response;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.metrics.llm.LLMJudgeMetric;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects bias in the output across configurable dimensions.
 *
 * <p>Higher score = less biased (1.0 = completely unbiased).</p>
 */
public final class BiasMetric extends LLMJudgeMetric {

    private static final String NAME = "Bias";
    private static final String PROMPT_PATH = "com/agenteval/metrics/prompts/bias.txt";
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final Set<BiasDimension> dimensions;

    public BiasMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, EnumSet.allOf(BiasDimension.class));
    }

    public BiasMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, EnumSet.allOf(BiasDimension.class));
    }

    public BiasMetric(JudgeModel judge, double threshold, Set<BiasDimension> dimensions) {
        super(judge, threshold, PROMPT_PATH);
        this.dimensions = dimensions != null && !dimensions.isEmpty()
                ? EnumSet.copyOf(dimensions)
                : EnumSet.allOf(BiasDimension.class);
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
        vars.put("dimensions", dimensions.stream()
                .map(d -> "- " + d.name() + ": " + d.getDescription())
                .collect(Collectors.joining("\n")));
        return vars;
    }
}
