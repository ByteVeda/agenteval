package org.byteveda.agenteval.reporting.regression.rootcause;

import java.util.List;

/**
 * Root cause analysis report for regression failures.
 *
 * @param clusters clusters of regressed cases grouped by shared failure patterns
 * @param summary human-readable summary of the root cause analysis
 * @param totalRegressedCases total number of regressed cases analyzed
 */
public record RootCauseReport(
        List<RegressionCluster> clusters,
        String summary,
        int totalRegressedCases
) {
    public RootCauseReport {
        clusters = List.copyOf(clusters);
    }
}
