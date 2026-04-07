package org.byteveda.agenteval.statistics.descriptive;

import java.util.Arrays;

/**
 * Pure static calculator for descriptive statistics on arrays of double values.
 *
 * <p>All methods are stateless and thread-safe. Uses Bessel's correction for
 * sample variance and the adjusted Fisher-Pearson coefficient for skewness.</p>
 */
public final class DescriptiveCalculator {

    private DescriptiveCalculator() {
        // utility class
    }

    /**
     * Computes comprehensive descriptive statistics for the given values.
     *
     * @param metricName the metric name for labeling
     * @param values the data values (must have at least 1 element)
     * @param cvThreshold the coefficient of variation threshold for high-variance flagging
     * @return a fully populated {@link DescriptiveStatistics} record
     * @throws IllegalArgumentException if values is empty
     */
    public static DescriptiveStatistics compute(String metricName, double[] values,
                                                double cvThreshold) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }

        double[] sorted = values.clone();
        Arrays.sort(sorted);

        int n = sorted.length;
        double mean = mean(sorted);
        double median = percentile(sorted, 0.50);
        double variance = variance(sorted, mean);
        double stdDev = Math.sqrt(variance);
        double min = sorted[0];
        double max = sorted[n - 1];
        double skewness = skewness(sorted, mean, stdDev);
        double kurtosis = kurtosis(sorted, mean, stdDev);
        double p5 = percentile(sorted, 0.05);
        double p25 = percentile(sorted, 0.25);
        double p50 = median;
        double p75 = percentile(sorted, 0.75);
        double p95 = percentile(sorted, 0.95);
        double cv = mean == 0.0 ? 0.0 : Math.abs(stdDev / mean);
        boolean highVariance = cv > cvThreshold;

        return new DescriptiveStatistics(
                metricName, n, mean, median, stdDev, variance,
                min, max, skewness, kurtosis,
                p5, p25, p50, p75, p95,
                cv, highVariance
        );
    }

    /**
     * Arithmetic mean.
     */
    static double mean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    /**
     * Sample variance with Bessel's correction (n-1 denominator).
     * Returns 0.0 for single-element arrays.
     */
    static double variance(double[] values, double mean) {
        if (values.length <= 1) {
            return 0.0;
        }
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return sumSq / (values.length - 1);
    }

    /**
     * Adjusted Fisher-Pearson skewness coefficient.
     * Returns 0.0 if n &lt; 3 or standard deviation is zero.
     */
    static double skewness(double[] values, double mean, double stdDev) {
        int n = values.length;
        if (n < 3 || stdDev == 0.0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            double z = (v - mean) / stdDev;
            sum += z * z * z;
        }
        double factor = (double) n / ((n - 1) * (n - 2));
        return factor * sum;
    }

    /**
     * Excess kurtosis (Fisher definition, normal = 0).
     * Returns 0.0 if n &lt; 4 or standard deviation is zero.
     */
    static double kurtosis(double[] values, double mean, double stdDev) {
        int n = values.length;
        if (n < 4 || stdDev == 0.0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            double z = (v - mean) / stdDev;
            sum += z * z * z * z;
        }
        double n1 = n - 1;
        double n2 = n - 2;
        double n3 = n - 3;
        double term1 = ((double) n * (n + 1)) / (n1 * n2 * n3) * sum;
        double term2 = 3.0 * n1 * n1 / (n2 * n3);
        return term1 - term2;
    }

    /**
     * Percentile using linear interpolation between closest ranks.
     *
     * @param sorted sorted array of values
     * @param p percentile as a fraction (e.g., 0.50 for median)
     * @return the interpolated percentile value
     */
    static double percentile(double[] sorted, double p) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double index = p * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted[lower];
        }
        double fraction = index - lower;
        return sorted[lower] + fraction * (sorted[upper] - sorted[lower]);
    }
}
