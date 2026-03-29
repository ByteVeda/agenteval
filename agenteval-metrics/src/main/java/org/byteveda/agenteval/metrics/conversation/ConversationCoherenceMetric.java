package org.byteveda.agenteval.metrics.conversation;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.ConversationTestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates whether a multi-turn conversation maintains coherence
 * across all turns.
 *
 * <p>Higher score = more coherent conversation (1.0 = perfectly coherent).</p>
 */
public final class ConversationCoherenceMetric extends LLMConversationMetric {

    private static final String NAME = "ConversationCoherence";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/conversation-coherence.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public ConversationCoherenceMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public ConversationCoherenceMetric(JudgeModel judge, double threshold) {
        super(judge, threshold, PROMPT_PATH);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected Map<String, String> buildTemplateVariables(ConversationTestCase testCase) {
        Map<String, String> vars = new HashMap<>();
        vars.put("systemPrompt", testCase.getSystemPrompt() != null
                ? testCase.getSystemPrompt() : "(none)");
        vars.put("turns", formatTurns(testCase.getTurns()));
        return vars;
    }
}
