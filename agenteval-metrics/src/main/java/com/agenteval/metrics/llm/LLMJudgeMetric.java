package com.agenteval.metrics.llm;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import com.agenteval.core.template.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for LLM-as-judge metrics.
 *
 * <p>Implements the template method pattern with a final {@link #evaluate(AgentTestCase)}
 * method that enforces the lifecycle:
 * <ol>
 *   <li>{@link #validate(AgentTestCase)} — check required fields</li>
 *   <li>{@link #buildTemplateVariables(AgentTestCase)} — extract template variables</li>
 *   <li>Render prompt template with variables</li>
 *   <li>Call judge LLM</li>
 *   <li>Return {@link EvalScore}</li>
 * </ol>
 * Subclasses override {@link #buildTemplateVariables(AgentTestCase)} and optionally
 * {@link #validate(AgentTestCase)}.</p>
 */
public abstract class LLMJudgeMetric implements EvalMetric {

    private static final Logger LOG = LoggerFactory.getLogger(LLMJudgeMetric.class);

    protected final JudgeModel judge;
    protected final double threshold;
    private final String promptResourcePath;

    protected LLMJudgeMetric(JudgeModel judge, double threshold, String promptResourcePath) {
        this.judge = Objects.requireNonNull(judge, "judge must not be null");
        this.promptResourcePath = Objects.requireNonNull(promptResourcePath,
                "promptResourcePath must not be null");
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        this.threshold = threshold;
    }

    @Override
    public final EvalScore evaluate(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");
        validate(testCase);

        Map<String, String> variables = buildTemplateVariables(testCase);
        String prompt = PromptTemplate.loadAndRender(promptResourcePath, variables);

        LOG.debug("Evaluating {} for input: {}",
                name(), truncate(testCase.getInput(), 100));

        JudgeResponse response = judge.judge(prompt);

        LOG.debug("{} scored {}: {}", name(), response.score(), response.reason());

        return EvalScore.of(response.score(), threshold, response.reason());
    }

    /**
     * Validates that the test case has the required fields for this metric.
     * Default implementation checks that input and actualOutput are present.
     *
     * @throws IllegalArgumentException if validation fails
     */
    protected void validate(AgentTestCase testCase) {
        if (testCase.getInput() == null || testCase.getInput().isBlank()) {
            throw new IllegalArgumentException(name() + " requires non-empty input");
        }
        if (testCase.getActualOutput() == null || testCase.getActualOutput().isBlank()) {
            throw new IllegalArgumentException(name() + " requires non-empty actualOutput");
        }
    }

    /**
     * Builds the map of template variables from the test case.
     * Keys correspond to {@code {{variable}}} placeholders in the prompt template.
     */
    protected abstract Map<String, String> buildTemplateVariables(AgentTestCase testCase);

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
