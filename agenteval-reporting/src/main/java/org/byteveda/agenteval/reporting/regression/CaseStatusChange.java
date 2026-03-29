package org.byteveda.agenteval.reporting.regression;

/**
 * Status change for a test case between baseline and current runs.
 *
 * @param input the test case input
 * @param baselinePassed whether the case passed in the baseline
 * @param currentPassed whether the case passes in the current run
 * @param metricDeltas per-metric score changes for this case
 */
public record CaseStatusChange(
        String input,
        boolean baselinePassed,
        boolean currentPassed,
        java.util.List<MetricDelta> metricDeltas
) {
    /**
     * Returns true if this case changed from pass to fail.
     */
    public boolean newFailure() {
        return baselinePassed && !currentPassed;
    }

    /**
     * Returns true if this case changed from fail to pass.
     */
    public boolean newPass() {
        return !baselinePassed && currentPassed;
    }
}
