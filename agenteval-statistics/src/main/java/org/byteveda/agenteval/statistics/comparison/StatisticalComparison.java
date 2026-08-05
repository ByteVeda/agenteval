package org.byteveda.agenteval.statistics.comparison;

import org.byteveda.agenteval.statistics.descriptive.DescriptiveStatistics;
import org.byteveda.agenteval.statistics.inference.EffectSize;
import org.byteveda.agenteval.statistics.inference.SignificanceTest;

/**
 * Per-metric statistical comparison between a baseline and current evaluation run.
 *
 * @param metricName the metric being compared
 * @param baselineStats descriptive statistics for the baseline scores
 * @param currentStats descriptive statistics for the current scores
 * @param delta the difference in means (current - baseline)
 * @param significanceTest the result of the significance test
 * @param effectSize the effect size measurement
 */
public record StatisticalComparison(
        String metricName,
        DescriptiveStatistics baselineStats,
        DescriptiveStatistics currentStats,
        double delta,
        SignificanceTest significanceTest,
        EffectSize effectSize
) {
}
