package org.byteveda.agenteval.fingerprint;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Complete capability profile for an agent, containing scores across all dimensions.
 *
 * @param agentName  the name of the profiled agent
 * @param scores     scores keyed by dimension
 * @param durationMs total profiling time in milliseconds
 */
public record CapabilityProfile(
        String agentName,
        Map<CapabilityDimension, ProfileScore> scores,
        long durationMs
) {

    public CapabilityProfile {
        Objects.requireNonNull(agentName, "agentName must not be null");
        Objects.requireNonNull(scores, "scores must not be null");
        scores = Map.copyOf(scores);
    }

    /**
     * Returns the overall score as the average across all dimensions.
     *
     * @return the average score (0.0 to 1.0), or 0.0 if no scores
     */
    public double overallScore() {
        if (scores.isEmpty()) {
            return 0.0;
        }
        return scores.values().stream()
                .mapToDouble(ProfileScore::score)
                .average()
                .orElse(0.0);
    }

    /**
     * Returns dimensions where the score is at or above the given threshold.
     *
     * @param threshold the minimum score to qualify as a strength
     * @return list of strong dimensions
     */
    public List<CapabilityDimension> strengths(double threshold) {
        return scores.entrySet().stream()
                .filter(e -> e.getValue().score() >= threshold)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Returns dimensions with strengths at or above 0.8.
     *
     * @return list of strong dimensions
     */
    public List<CapabilityDimension> strengths() {
        return strengths(0.8);
    }

    /**
     * Returns dimensions where the score is below the given threshold.
     *
     * @param threshold the score below which a dimension is considered weak
     * @return list of weak dimensions
     */
    public List<CapabilityDimension> weaknesses(double threshold) {
        return scores.entrySet().stream()
                .filter(e -> e.getValue().score() < threshold)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Returns dimensions with weaknesses below 0.5.
     *
     * @return list of weak dimensions
     */
    public List<CapabilityDimension> weaknesses() {
        return weaknesses(0.5);
    }
}
