package org.byteveda.agenteval.statistics.report;

import org.byteveda.agenteval.statistics.descriptive.DescriptiveStatistics;
import org.byteveda.agenteval.statistics.inference.ConfidenceInterval;
import org.byteveda.agenteval.statistics.inference.NormalityTest;

/**
 * Combined statistical analysis for a single metric, grouping descriptive statistics,
 * confidence interval, and normality test results.
 *
 * @param metricName the metric name
 * @param descriptive descriptive statistics for the metric's scores
 * @param confidenceInterval confidence interval for the metric's mean score
 * @param normality normality test result (may be null if not enough data)
 */
public record MetricStatistics(
        String metricName,
        DescriptiveStatistics descriptive,
        ConfidenceInterval confidenceInterval,
        NormalityTest normality
) {
}
