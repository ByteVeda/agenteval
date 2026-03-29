package org.byteveda.agenteval.reporting.regression;

/**
 * Score change for a single metric between baseline and current runs.
 *
 * @param metricName the metric name
 * @param baselineScore the baseline score
 * @param currentScore the current score
 * @param delta the change (current - baseline)
 */
public record MetricDelta(
        String metricName,
        double baselineScore,
        double currentScore,
        double delta
) {
    /**
     * Returns true if the metric score improved (positive delta).
     */
    public boolean improved() {
        return delta > 0;
    }

    /**
     * Returns true if the metric score regressed (negative delta).
     */
    public boolean regressed() {
        return delta < 0;
    }
}
