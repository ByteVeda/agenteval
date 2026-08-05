package org.byteveda.agenteval.statistics.inference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link InferenceCalculator}.
 */
class InferenceCalculatorTest {

    private static final double TOLERANCE = 1e-4;

    // --- Confidence Intervals ---

    @Test
    void tConfidenceIntervalContainsTrueMean() {
        // Known population: mean=50, we sample around it
        double[] data = {48.0, 49.0, 50.0, 51.0, 52.0};
        ConfidenceInterval ci = InferenceCalculator.tConfidenceInterval(data,
                ConfidenceLevel.P95);

        assertEquals(0.95, ci.level(), TOLERANCE);
        assertEquals(50.0, ci.pointEstimate(), TOLERANCE);
        assertTrue(ci.lower() < 50.0, "Lower bound should be below true mean");
        assertTrue(ci.upper() > 50.0, "Upper bound should be above true mean");
        assertEquals("t-distribution", ci.method());
    }

    @Test
    void tConfidenceIntervalWidensWithHigherConfidence() {
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};

        ConfidenceInterval ci90 = InferenceCalculator.tConfidenceInterval(data,
                ConfidenceLevel.P90);
        ConfidenceInterval ci95 = InferenceCalculator.tConfidenceInterval(data,
                ConfidenceLevel.P95);
        ConfidenceInterval ci99 = InferenceCalculator.tConfidenceInterval(data,
                ConfidenceLevel.P99);

        assertTrue(ci90.width() < ci95.width(),
                "90% CI should be narrower than 95% CI");
        assertTrue(ci95.width() < ci99.width(),
                "95% CI should be narrower than 99% CI");
    }

    @Test
    void tConfidenceIntervalRejectsFewerThanTwoValues() {
        assertThrows(IllegalArgumentException.class,
                () -> InferenceCalculator.tConfidenceInterval(new double[]{1.0},
                        ConfidenceLevel.P95));
    }

    @Test
    void bootstrapConfidenceIntervalContainsMean() {
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        ConfidenceInterval ci = InferenceCalculator.bootstrapConfidenceInterval(data,
                ConfidenceLevel.P95, 10_000);

        assertEquals(5.5, ci.pointEstimate(), TOLERANCE);
        assertTrue(ci.lower() <= 5.5, "Lower bound should be at or below mean");
        assertTrue(ci.upper() >= 5.5, "Upper bound should be at or above mean");
        assertEquals("bootstrap-percentile", ci.method());
    }

    @Test
    void bootstrapConfidenceIntervalRejectsEmptyArray() {
        assertThrows(IllegalArgumentException.class,
                () -> InferenceCalculator.bootstrapConfidenceInterval(new double[0],
                        ConfidenceLevel.P95, 1000));
    }

    // --- Paired t-test ---

    @Test
    void pairedTTestWithIdenticalArraysNotSignificant() {
        double[] data = {0.8, 0.85, 0.9, 0.87, 0.82};
        SignificanceTest result = InferenceCalculator.pairedTTest(data, data, 0.05);

        assertEquals("Paired t-test", result.testName());
        assertTrue(result.pValue() >= 0.99,
                "Identical arrays should yield p close to 1.0, got: " + result.pValue());
        assertFalse(result.significant(),
                "Identical arrays should not be significant");
    }

    @Test
    void pairedTTestWithClearDifference() {
        double[] baseline = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
        double[] current = {0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9};
        SignificanceTest result = InferenceCalculator.pairedTTest(baseline, current, 0.05);

        assertTrue(result.significant(),
                "Large consistent difference should be significant");
        assertTrue(result.pValue() < 0.001,
                "p-value should be very small for clear difference");
    }

    @Test
    void pairedTTestRejectsMismatchedLengths() {
        assertThrows(IllegalArgumentException.class,
                () -> InferenceCalculator.pairedTTest(
                        new double[]{1.0, 2.0}, new double[]{1.0}, 0.05));
    }

    @Test
    void pairedTTestRejectsTooFewElements() {
        assertThrows(IllegalArgumentException.class,
                () -> InferenceCalculator.pairedTTest(
                        new double[]{1.0}, new double[]{2.0}, 0.05));
    }

    // --- Wilcoxon signed-rank ---

    @Test
    void wilcoxonWithIdenticalArraysNotSignificant() {
        double[] data = {0.7, 0.75, 0.8, 0.72, 0.78, 0.71, 0.79, 0.74, 0.76, 0.73};
        SignificanceTest result = InferenceCalculator.wilcoxonSignedRank(data, data, 0.05);

        assertFalse(result.significant(),
                "Identical arrays should not be significant");
        assertEquals("Wilcoxon signed-rank test", result.testName());
    }

    @Test
    void wilcoxonWithClearDifference() {
        double[] baseline = {0.3, 0.32, 0.31, 0.29, 0.33, 0.30, 0.28, 0.34, 0.31, 0.30};
        double[] current = {0.8, 0.82, 0.81, 0.79, 0.83, 0.80, 0.78, 0.84, 0.81, 0.80};
        SignificanceTest result = InferenceCalculator.wilcoxonSignedRank(baseline, current, 0.05);

        assertTrue(result.significant(),
                "Large consistent difference should be significant in Wilcoxon test");
    }

    @Test
    void wilcoxonRejectsTooFewElements() {
        assertThrows(IllegalArgumentException.class,
                () -> InferenceCalculator.wilcoxonSignedRank(
                        new double[]{1, 2, 3, 4, 5}, new double[]{2, 3, 4, 5, 6}, 0.05));
    }

    // --- Effect Size ---

    @Test
    void effectSizeNegligible() {
        // Ensure differences are tiny relative to variance so |d| < 0.2
        double[] baseline = {0.80, 0.85, 0.82, 0.78, 0.81, 0.83, 0.79, 0.84, 0.80, 0.82};
        double[] current =  {0.81, 0.85, 0.82, 0.79, 0.81, 0.83, 0.79, 0.84, 0.81, 0.82};
        EffectSize result = InferenceCalculator.cohensD(baseline, current);

        assertTrue(Math.abs(result.cohensD()) < 0.2,
                "Cohen's d should be < 0.2, got: " + result.cohensD());
        assertEquals(EffectSize.Magnitude.NEGLIGIBLE, result.magnitude(),
                "Very small difference should be negligible, got d=" + result.cohensD());
    }

    @Test
    void effectSizeLarge() {
        double[] baseline = {0.3, 0.35, 0.32, 0.31, 0.33};
        double[] current = {0.8, 0.85, 0.82, 0.81, 0.83};
        EffectSize result = InferenceCalculator.cohensD(baseline, current);

        assertEquals(EffectSize.Magnitude.LARGE, result.magnitude(),
                "Large difference should have large effect size");
        assertTrue(result.cohensD() > 0,
                "Positive direction should have positive Cohen's d");
    }

    @Test
    void effectSizeIdenticalArraysReturnsZero() {
        double[] data = {0.5, 0.6, 0.55, 0.52, 0.58};
        EffectSize result = InferenceCalculator.cohensD(data, data);

        assertEquals(0.0, result.cohensD(), TOLERANCE);
        assertEquals(EffectSize.Magnitude.NEGLIGIBLE, result.magnitude());
    }

    @Test
    void effectSizeClassifyThresholds() {
        assertEquals(EffectSize.Magnitude.NEGLIGIBLE, EffectSize.classify(0.0));
        assertEquals(EffectSize.Magnitude.NEGLIGIBLE, EffectSize.classify(0.19));
        assertEquals(EffectSize.Magnitude.SMALL, EffectSize.classify(0.2));
        assertEquals(EffectSize.Magnitude.SMALL, EffectSize.classify(0.49));
        assertEquals(EffectSize.Magnitude.MEDIUM, EffectSize.classify(0.5));
        assertEquals(EffectSize.Magnitude.MEDIUM, EffectSize.classify(0.79));
        assertEquals(EffectSize.Magnitude.LARGE, EffectSize.classify(0.8));
        assertEquals(EffectSize.Magnitude.LARGE, EffectSize.classify(2.0));
    }

    @Test
    void effectSizeClassifyNegativeValues() {
        // Should use absolute value
        assertEquals(EffectSize.Magnitude.LARGE, EffectSize.classify(-1.0));
        assertEquals(EffectSize.Magnitude.SMALL, EffectSize.classify(-0.3));
    }

    // --- Sample Size Recommendation ---

    @Test
    void recommendSampleSizeForSmallEffect() {
        SampleSizeRecommendation rec = InferenceCalculator.recommendSampleSize(0.2, 0.05, 0.80);

        assertTrue(rec.recommendedSampleSize() > 100,
                "Small effect should require many samples");
        assertEquals(0.05, rec.desiredAlpha(), TOLERANCE);
        assertEquals(0.80, rec.desiredPower(), TOLERANCE);
    }

    @Test
    void recommendSampleSizeForLargeEffect() {
        SampleSizeRecommendation rec = InferenceCalculator.recommendSampleSize(0.8, 0.05, 0.80);

        assertTrue(rec.recommendedSampleSize() < 50,
                "Large effect should require fewer samples");
    }

    @Test
    void recommendSampleSizeForNegligibleEffect() {
        SampleSizeRecommendation rec = InferenceCalculator.recommendSampleSize(0.001, 0.05, 0.80);

        assertTrue(rec.recommendedSampleSize() >= 1000,
                "Negligible effect should result in very large recommendation");
    }

    @Test
    void recommendSampleSizeLargerNeedsHigherPower() {
        SampleSizeRecommendation rec80 = InferenceCalculator.recommendSampleSize(0.5, 0.05, 0.80);
        SampleSizeRecommendation rec90 = InferenceCalculator.recommendSampleSize(0.5, 0.05, 0.90);

        assertTrue(rec90.recommendedSampleSize() > rec80.recommendedSampleSize(),
                "Higher power should require more samples");
    }
}
