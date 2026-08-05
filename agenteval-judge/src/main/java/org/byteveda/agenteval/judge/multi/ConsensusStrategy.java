package org.byteveda.agenteval.judge.multi;

/**
 * Strategy for aggregating scores from multiple judge models.
 */
public enum ConsensusStrategy {

    /** Score passes if more than half of judges agree (score >= threshold). */
    MAJORITY,

    /** Final score is the arithmetic mean of all judge scores. */
    AVERAGE,

    /** Final score is the weighted mean of all judge scores. */
    WEIGHTED_AVERAGE,

    /** Score passes only if all judges agree (score >= threshold). */
    UNANIMOUS
}
