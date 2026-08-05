package org.byteveda.agenteval.statistics.report;

import org.byteveda.agenteval.statistics.descriptive.DescriptiveStatistics;
import org.byteveda.agenteval.statistics.inference.ConfidenceInterval;
import org.byteveda.agenteval.statistics.inference.SampleSizeRecommendation;

import java.util.List;
import java.util.Map;

/**
 * Top-level statistical report for an evaluation run.
 *
 * @param metricStatistics per-metric statistical analyses
 * @param overallDescriptive descriptive statistics across all scores
 * @param overallConfidenceInterval confidence interval for the overall mean
 * @param warnings list of statistical warnings (e.g., high variance, small sample size)
 * @param sampleSizeRecommendation recommendation for future sample sizes (may be null)
 */
public record StatisticalReport(
        Map<String, MetricStatistics> metricStatistics,
        DescriptiveStatistics overallDescriptive,
        ConfidenceInterval overallConfidenceInterval,
        List<String> warnings,
        SampleSizeRecommendation sampleSizeRecommendation
) {
    /**
     * Compact constructor ensuring immutable collections.
     */
    public StatisticalReport {
        metricStatistics = Map.copyOf(metricStatistics);
        warnings = List.copyOf(warnings);
    }
}
