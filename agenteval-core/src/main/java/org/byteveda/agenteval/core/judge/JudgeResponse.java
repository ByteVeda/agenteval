package org.byteveda.agenteval.core.judge;

import org.byteveda.agenteval.core.model.TokenUsage;

/**
 * Response from an LLM judge evaluation.
 */
public record JudgeResponse(
        double score,
        String reason,
        TokenUsage tokenUsage
) {
    public JudgeResponse {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0.0 and 1.0, got: " + score);
        }
        if (reason == null) {
            reason = "";
        }
    }
}
