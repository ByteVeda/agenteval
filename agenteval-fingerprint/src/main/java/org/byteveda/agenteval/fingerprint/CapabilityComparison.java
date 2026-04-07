package org.byteveda.agenteval.fingerprint;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility for comparing two {@link CapabilityProfile} instances.
 */
public final class CapabilityComparison {

    private CapabilityComparison() {}

    /**
     * Compares two capability profiles and returns a comparison result.
     *
     * <p>For each dimension present in both profiles, computes the delta
     * (B minus A). Positive deltas indicate improvement in profile B;
     * negative deltas indicate regression.</p>
     *
     * @param profileA the baseline profile
     * @param profileB the profile to compare against the baseline
     * @return the comparison result
     */
    public static CapabilityComparisonResult compare(
            CapabilityProfile profileA, CapabilityProfile profileB) {
        Objects.requireNonNull(profileA, "profileA must not be null");
        Objects.requireNonNull(profileB, "profileB must not be null");

        Map<CapabilityDimension, Double> deltas = new EnumMap<>(CapabilityDimension.class);
        List<CapabilityDimension> improvements = new ArrayList<>();
        List<CapabilityDimension> regressions = new ArrayList<>();

        Set<CapabilityDimension> allDimensions = profileA.scores().keySet();

        for (CapabilityDimension dim : allDimensions) {
            ProfileScore scoreA = profileA.scores().get(dim);
            ProfileScore scoreB = profileB.scores().get(dim);

            if (scoreA != null && scoreB != null) {
                double delta = scoreB.score() - scoreA.score();
                deltas.put(dim, delta);

                if (delta > 0.0) {
                    improvements.add(dim);
                } else if (delta < 0.0) {
                    regressions.add(dim);
                }
            }
        }

        // Also check dimensions only in B
        for (CapabilityDimension dim : profileB.scores().keySet()) {
            if (!deltas.containsKey(dim)) {
                ProfileScore scoreB = profileB.scores().get(dim);
                deltas.put(dim, scoreB.score());
                improvements.add(dim);
            }
        }

        return new CapabilityComparisonResult(
                profileA, profileB, deltas, improvements, regressions
        );
    }
}
