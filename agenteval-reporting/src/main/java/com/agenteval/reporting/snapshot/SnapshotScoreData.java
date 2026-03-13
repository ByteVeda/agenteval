package com.agenteval.reporting.snapshot;

/**
 * Jackson-serializable mirror of {@link com.agenteval.core.model.EvalScore}
 * for snapshot persistence.
 *
 * @param value     the score value (0.0–1.0)
 * @param threshold the pass/fail threshold
 * @param passed    whether the score met the threshold
 * @param reason    the reason for the score
 */
public record SnapshotScoreData(
        double value,
        double threshold,
        boolean passed,
        String reason
) {
}
