package org.byteveda.agenteval.statistics.stability;

import java.util.List;
import java.util.Map;

/**
 * Stability analysis across multiple evaluation runs, assessing consistency
 * per metric and overall.
 *
 * @param numberOfRuns the total number of evaluation runs analyzed
 * @param metricConsistency per-metric consistency results
 * @param overallConsistency overall consistency across all metrics
 * @param warnings list of stability-related warnings
 */
public record StabilityAnalysis(
        int numberOfRuns,
        Map<String, RunConsistency> metricConsistency,
        RunConsistency overallConsistency,
        List<String> warnings
) {
    /**
     * Compact constructor ensuring immutable collections.
     */
    public StabilityAnalysis {
        metricConsistency = Map.copyOf(metricConsistency);
        warnings = List.copyOf(warnings);
    }
}
