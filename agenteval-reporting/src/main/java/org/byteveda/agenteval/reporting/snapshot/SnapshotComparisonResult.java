package org.byteveda.agenteval.reporting.snapshot;

import org.byteveda.agenteval.reporting.regression.RegressionReport;

import java.util.Objects;

/**
 * Result of comparing current evaluation results against a saved snapshot.
 *
 * @param snapshotName     the name of the snapshot compared against
 * @param status           the comparison status
 * @param regressionReport the detailed regression report
 */
public record SnapshotComparisonResult(
        String snapshotName,
        SnapshotStatus status,
        RegressionReport regressionReport
) {
    public SnapshotComparisonResult {
        Objects.requireNonNull(snapshotName, "snapshotName must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
