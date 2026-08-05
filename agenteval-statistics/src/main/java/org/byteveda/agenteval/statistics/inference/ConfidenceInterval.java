package org.byteveda.agenteval.statistics.inference;

/**
 * A confidence interval for a population parameter.
 *
 * @param lower lower bound of the interval
 * @param upper upper bound of the interval
 * @param level confidence level (e.g., 0.95)
 * @param pointEstimate the point estimate (e.g., sample mean)
 * @param method the method used (e.g., "t-distribution", "bootstrap-percentile")
 */
public record ConfidenceInterval(
        double lower,
        double upper,
        double level,
        double pointEstimate,
        String method
) {

    /**
     * Returns the width of the confidence interval.
     *
     * @return upper minus lower
     */
    public double width() {
        return upper - lower;
    }

    /**
     * Returns the margin of error (half the interval width).
     *
     * @return half the width
     */
    public double marginOfError() {
        return width() / 2.0;
    }
}
