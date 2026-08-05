package org.byteveda.agenteval.reporting.regression.rootcause;

/**
 * Types of failure patterns detected during regression root cause analysis.
 */
public enum PatternType {

    /** Significant change in output length between baseline and current. */
    OUTPUT_LENGTH_CHANGE,

    /** Change in tool usage patterns (different tools called, different counts). */
    TOOL_USAGE_CHANGE,

    /** One or more metrics regressed beyond a significance threshold. */
    METRIC_REGRESSION,

    /** Cost increased significantly between baseline and current. */
    COST_INCREASE,

    /** Latency increased significantly between baseline and current. */
    LATENCY_INCREASE
}
