package com.agenteval.metrics.conversation;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.model.ConversationTestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates whether the agent retains and correctly uses context
 * from earlier turns in a multi-turn conversation.
 *
 * <p>Higher score = better context retention (1.0 = perfect retention).</p>
 */
public final class ContextRetentionMetric extends LLMConversationMetric {

    private static final String NAME = "ContextRetention";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/context-retention.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;

    public ContextRetentionMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public ContextRetentionMetric(JudgeModel judge, double threshold) {
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
