package org.byteveda.agenteval.fingerprint;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of comparing two capability profiles.
 *
 * @param profileA    the first profile
 * @param profileB    the second profile
 * @param deltas      score differences per dimension (B minus A)
 * @param improvements dimensions where B scored higher than A
 * @param regressions  dimensions where B scored lower than A
 */
public record CapabilityComparisonResult(
        CapabilityProfile profileA,
        CapabilityProfile profileB,
        Map<CapabilityDimension, Double> deltas,
        List<CapabilityDimension> improvements,
        List<CapabilityDimension> regressions
) {

    public CapabilityComparisonResult {
        Objects.requireNonNull(profileA, "profileA must not be null");
        Objects.requireNonNull(profileB, "profileB must not be null");
        Objects.requireNonNull(deltas, "deltas must not be null");
        deltas = Map.copyOf(deltas);
        improvements = improvements == null ? List.of() : List.copyOf(improvements);
        regressions = regressions == null ? List.of() : List.copyOf(regressions);
    }

    /**
     * Returns the overall score delta (B minus A).
     *
     * @return the overall delta
     */
    public double overallDelta() {
        return profileB.overallScore() - profileA.overallScore();
    }
}
