package org.byteveda.agenteval.metrics.conversation;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.ConversationTestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM-as-judge conversation metric that evaluates whether a conversation
 * reached a satisfactory resolution.
 *
 * <p>Optionally accepts custom success criteria to guide the judge's evaluation.</p>
 */
public final class ConversationResolutionMetric extends LLMConversationMetric {

    private static final String NAME = "ConversationResolution";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/conversation-resolution.txt";
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final String successCriteria;

    public ConversationResolutionMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, null);
    }

    public ConversationResolutionMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, null);
    }

    public ConversationResolutionMetric(JudgeModel judge, double threshold,
                                        String successCriteria) {
        super(judge, threshold, PROMPT_PATH);
        this.successCriteria = successCriteria;
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
        vars.put("successCriteria", successCriteria != null
                ? successCriteria : "(not specified)");
        return vars;
    }
}
