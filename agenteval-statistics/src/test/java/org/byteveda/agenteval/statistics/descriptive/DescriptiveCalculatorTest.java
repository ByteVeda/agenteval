package org.byteveda.agenteval.statistics.descriptive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DescriptiveCalculator} using known datasets.
 */
class DescriptiveCalculatorTest {

    private static final double TOLERANCE = 1e-6;

    @Test
    void computeWithSimpleDataset() {
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);

        assertEquals("test", stats.metricName());
        assertEquals(5, stats.n());
        assertEquals(3.0, stats.mean(), TOLERANCE);
        assertEquals(3.0, stats.median(), TOLERANCE);
        assertEquals(2.5, stats.variance(), TOLERANCE);
        assertEquals(Math.sqrt(2.5), stats.standardDeviation(), TOLERANCE);
        assertEquals(1.0, stats.min(), TOLERANCE);
        assertEquals(5.0, stats.max(), TOLERANCE);
    }

    @Test
    void computeMedianOddCount() {
        double[] data = {3.0, 1.0, 2.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);
        assertEquals(2.0, stats.median(), TOLERANCE);
    }

    @Test
    void computeMedianEvenCount() {
        double[] data = {1.0, 2.0, 3.0, 4.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);
        assertEquals(2.5, stats.median(), TOLERANCE);
    }

    @Test
    void computePercentilesLinearInterpolation() {
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);

        // p(k) = k * (n-1) index, linear interpolation
        // p5:  0.05 * 4 = 0.2 -> 1 + 0.2*(2-1) = 1.2
        assertEquals(1.2, stats.p5(), TOLERANCE);
        assertEquals(2.0, stats.p25(), TOLERANCE);
        assertEquals(3.0, stats.p50(), TOLERANCE);
        assertEquals(4.0, stats.p75(), TOLERANCE);
        // p95: 0.95 * 4 = 3.8 -> 4 + 0.8*(5-4) = 4.8
        assertEquals(4.8, stats.p95(), TOLERANCE);
    }

    @Test
    void computeVarianceWithBesselsCorrection() {
        // Population variance of [2,4,4,4,5,5,7,9] = 4.0
        // Sample variance = n/(n-1) * 4.0 = 8/7 * 4.0 = 4.571...
        double[] data = {2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);
        double expectedMean = 5.0;
        assertEquals(expectedMean, stats.mean(), TOLERANCE);

        double expectedVariance = 4.571428571;
        assertEquals(expectedVariance, stats.variance(), 0.001);
    }

    @Test
    void computeSingleValueVarianceIsZero() {
        double[] data = {42.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);

        assertEquals(42.0, stats.mean(), TOLERANCE);
        assertEquals(42.0, stats.median(), TOLERANCE);
        assertEquals(0.0, stats.variance(), TOLERANCE);
        assertEquals(0.0, stats.standardDeviation(), TOLERANCE);
        assertEquals(42.0, stats.min(), TOLERANCE);
        assertEquals(42.0, stats.max(), TOLERANCE);
    }

    @Test
    void computeSkewnessSymmetricDistribution() {
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);
        assertEquals(0.0, stats.skewness(), 0.01);
    }

    @Test
    void computeKurtosisForUniformLikeData() {
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);
        // Uniform distributions have negative excess kurtosis
        assertTrue(stats.kurtosis() < 0,
                "Uniform-like data should have negative excess kurtosis");
    }

    @Test
    void computeHighVarianceFlag() {
        // Data with high CV (stddev/mean > 0.15)
        double[] data = {0.1, 0.5, 0.9, 0.2, 0.8};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);
        assertTrue(stats.highVarianceFlag(),
                "High-variance data should be flagged");
    }

    @Test
    void computeLowVarianceNoFlag() {
        // Data with low CV
        double[] data = {0.90, 0.91, 0.92, 0.93, 0.94};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);
        assertFalse(stats.highVarianceFlag(),
                "Low-variance data should not be flagged");
    }

    @Test
    void computeCoefficientOfVariation() {
        double[] data = {10.0, 20.0, 30.0};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);
        double expectedCv = stats.standardDeviation() / stats.mean();
        assertEquals(expectedCv, stats.coefficientOfVariation(), TOLERANCE);
    }

    @Test
    void computeRejectsEmptyArray() {
        assertThrows(IllegalArgumentException.class,
                () -> DescriptiveCalculator.compute("test", new double[0], 0.15));
    }

    @Test
    void computeWithIdenticalValues() {
        double[] data = {0.5, 0.5, 0.5, 0.5};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);

        assertEquals(0.5, stats.mean(), TOLERANCE);
        assertEquals(0.5, stats.median(), TOLERANCE);
        assertEquals(0.0, stats.variance(), TOLERANCE);
        assertEquals(0.0, stats.standardDeviation(), TOLERANCE);
        assertEquals(0.0, stats.coefficientOfVariation(), TOLERANCE);
        assertFalse(stats.highVarianceFlag());
    }

    @Test
    void computeWithTwoValues() {
        double[] data = {0.3, 0.7};
        DescriptiveStatistics stats = DescriptiveCalculator.compute("test", data, 0.15);

        assertEquals(0.5, stats.mean(), TOLERANCE);
        assertEquals(0.5, stats.median(), TOLERANCE);
        assertEquals(2, stats.n());
    }
}
