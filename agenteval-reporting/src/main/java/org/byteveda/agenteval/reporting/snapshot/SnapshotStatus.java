package org.byteveda.agenteval.reporting.snapshot;

/**
 * Status of a snapshot comparison.
 */
public enum SnapshotStatus {
    /** New snapshot created (no prior baseline). */
    CREATED,
    /** Current results match the baseline snapshot. */
    MATCHED,
    /** Current results show regressions compared to baseline. */
    REGRESSED,
    /** Current results show improvement over baseline. */
    IMPROVED
}
