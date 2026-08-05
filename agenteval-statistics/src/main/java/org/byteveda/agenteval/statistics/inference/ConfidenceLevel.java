package org.byteveda.agenteval.statistics.inference;

/**
 * Standard confidence levels for statistical inference.
 */
public enum ConfidenceLevel {

    /** 90% confidence level. */
    P90(0.90),

    /** 95% confidence level. */
    P95(0.95),

    /** 99% confidence level. */
    P99(0.99);

    private final double level;

    ConfidenceLevel(double level) {
        this.level = level;
    }

    /**
     * Returns the numeric confidence level (e.g., 0.95 for 95%).
     *
     * @return the confidence level as a double
     */
    public double level() {
        return level;
    }
}
