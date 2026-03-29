package org.byteveda.agenteval.core.metric;

/**
 * Strategy for combining multiple metric scores in a {@link CompositeMetric}.
 */
public enum CompositeStrategy {

    /**
     * Final score is the weighted average of all metric scores.
     * Passes if the weighted average meets the composite threshold.
     */
    WEIGHTED_AVERAGE,

    /**
     * Passes only if all individual metrics pass their own thresholds.
     * Final score is the minimum score across all metrics.
     */
    ALL_PASS,

    /**
     * Passes if any individual metric passes its threshold.
     * Final score is the maximum score across all metrics.
     */
    ANY_PASS
}
