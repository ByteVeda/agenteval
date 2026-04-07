package org.byteveda.agenteval.statistics.stability;

/**
 * Consistency analysis for a single metric across multiple evaluation runs.
 *
 * @param metricName the metric name
 * @param numberOfRuns the number of runs analyzed
 * @param meanScore the mean score across runs
 * @param standardDeviation the standard deviation across runs
 * @param coefficientOfVariation the CV (stdDev / mean)
 * @param isStable true if the metric is considered stable (CV below threshold)
 * @param assessment human-readable stability assessment
 */
public record RunConsistency(
        String metricName,
        int numberOfRuns,
        double meanScore,
        double standardDeviation,
        double coefficientOfVariation,
        boolean isStable,
        String assessment
) {
}
