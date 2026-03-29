package org.byteveda.agenteval.core.model;

import java.util.Objects;

/**
 * The result of a metric evaluation. Score is normalized to 0.0–1.0.
 */
public record EvalScore(
        double value,
        double threshold,
        boolean passed,
        String reason,
        String metricName
) {
    public EvalScore {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("value must be between 0.0 and 1.0, got: " + value);
        }
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        Objects.requireNonNull(reason, "reason must not be null");
        if (metricName == null) {
            metricName = "";
        }
    }

    /**
     * Creates an EvalScore with auto-derived passed flag (value >= threshold).
     * metricName defaults to empty — populated by the evaluation engine.
     */
    public static EvalScore of(double value, double threshold, String reason) {
        return new EvalScore(value, threshold, value >= threshold, reason, "");
    }

    /**
     * Creates a passing EvalScore with perfect score.
     */
    public static EvalScore pass(String reason) {
        return new EvalScore(1.0, 0.0, true, reason, "");
    }

    /**
     * Creates a failing EvalScore with zero score.
     */
    public static EvalScore fail(String reason) {
        return new EvalScore(0.0, 1.0, false, reason, "");
    }

    /**
     * Returns a new EvalScore with the specified metric name.
     */
    public EvalScore withMetricName(String metricName) {
        return new EvalScore(value, threshold, passed, reason, metricName);
    }
}
