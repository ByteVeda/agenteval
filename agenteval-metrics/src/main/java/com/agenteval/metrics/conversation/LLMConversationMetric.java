package com.agenteval.metrics.conversation;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.metric.ConversationMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.ConversationTestCase;
import com.agenteval.core.model.EvalScore;
import com.agenteval.core.template.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for LLM-as-judge conversation metrics.
 *
 * <p>Follows the same template method pattern as {@code LLMJudgeMetric},
 * but operates on {@link ConversationTestCase} instead of {@code AgentTestCase}.</p>
 */
public abstract sealed class LLMConversationMetric implements ConversationMetric
        permits ConversationCoherenceMetric, ContextRetentionMetric,
                ConversationResolutionMetric {

    private static final Logger LOG = LoggerFactory.getLogger(LLMConversationMetric.class);

    protected final JudgeModel judge;
    protected final double threshold;
    private final String promptResourcePath;

    protected LLMConversationMetric(JudgeModel judge, double threshold,
                                    String promptResourcePath) {
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
    public final EvalScore evaluate(ConversationTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");
        validate(testCase);

        Map<String, String> variables = buildTemplateVariables(testCase);
        String prompt = PromptTemplate.loadAndRender(promptResourcePath, variables);

        LOG.debug("Evaluating {} for conversation: {}",
                name(), testCase.getConversationId());

        JudgeResponse response = judge.judge(prompt);

        LOG.debug("{} scored {}: {}", name(), response.score(), response.reason());

        return EvalScore.of(response.score(), threshold, response.reason());
    }

    /**
     * Validates that the conversation test case has the required fields.
     */
    protected void validate(ConversationTestCase testCase) {
        if (testCase.getTurns().isEmpty()) {
            throw new IllegalArgumentException(name() + " requires non-empty turns");
        }
    }

    /**
     * Builds the map of template variables from the conversation test case.
     */
    protected abstract Map<String, String> buildTemplateVariables(
            ConversationTestCase testCase);

    /**
     * Formats conversation turns for inclusion in prompts.
     */
    protected static String formatTurns(List<AgentTestCase> turns) {
        var sb = new StringBuilder();
        for (int i = 0; i < turns.size(); i++) {
            AgentTestCase turn = turns.get(i);
            sb.append("Turn ").append(i + 1).append(" [USER]: ")
                    .append(turn.getInput());
            if (turn.getActualOutput() != null) {
                sb.append("\nTurn ").append(i + 1).append(" [AGENT]: ")
                        .append(turn.getActualOutput());
            }
            if (i < turns.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
