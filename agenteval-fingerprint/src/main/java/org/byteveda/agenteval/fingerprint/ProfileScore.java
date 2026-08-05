package org.byteveda.agenteval.fingerprint;

import java.util.Objects;

/**
 * Score for a single capability dimension.
 *
 * @param dimension the capability dimension
 * @param score     the score (0.0 to 1.0)
 * @param reason    explanation of the score
 */
public record ProfileScore(
        CapabilityDimension dimension,
        double score,
        String reason
) {

    public ProfileScore {
        Objects.requireNonNull(dimension, "dimension must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(
                    "score must be between 0.0 and 1.0, got: " + score
            );
        }
    }
}
