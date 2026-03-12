package com.agenteval.reporting.regression;

import java.util.List;
import java.util.Map;

/**
 * Report comparing baseline and current evaluation results.
 *
 * @param overallBaselineScore baseline average score
 * @param overallCurrentScore current average score
 * @param overallDelta overall score change
 * @param metricDeltas per-metric average score changes
 * @param caseChanges per-case status changes (only cases present in both runs)
 * @param newFailures count of cases that regressed from pass to fail
 * @param newPasses count of cases that improved from fail to pass
 */
public record RegressionReport(
        double overallBaselineScore,
        double overallCurrentScore,
        double overallDelta,
        Map<String, MetricDelta> metricDeltas,
        List<CaseStatusChange> caseChanges,
        int newFailures,
        int newPasses
) {
    /**
     * Returns true if any regressions were detected.
     */
    public boolean hasRegressions() {
        return newFailures > 0 || overallDelta < 0;
    }
}
