package org.byteveda.agenteval.statistics.descriptive;

/**
 * Descriptive statistics summary for a set of metric scores.
 *
 * @param metricName the name of the metric
 * @param n sample size
 * @param mean arithmetic mean
 * @param median 50th percentile
 * @param standardDeviation sample standard deviation (with Bessel's correction)
 * @param variance sample variance (with Bessel's correction)
 * @param min minimum value
 * @param max maximum value
 * @param skewness adjusted Fisher-Pearson skewness coefficient
 * @param kurtosis excess kurtosis
 * @param p5 5th percentile
 * @param p25 25th percentile (Q1)
 * @param p50 50th percentile (same as median)
 * @param p75 75th percentile (Q3)
 * @param p95 95th percentile
 * @param coefficientOfVariation ratio of standard deviation to mean
 * @param highVarianceFlag true if CV exceeds the configured threshold
 */
public record DescriptiveStatistics(
        String metricName,
        int n,
        double mean,
        double median,
        double standardDeviation,
        double variance,
        double min,
        double max,
        double skewness,
        double kurtosis,
        double p5,
        double p25,
        double p50,
        double p75,
        double p95,
        double coefficientOfVariation,
        boolean highVarianceFlag
) {
}
