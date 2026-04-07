package org.byteveda.agenteval.statistics.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Known-answer tests for statistical distribution functions.
 */
class DistributionsTest {

    private static final double TOLERANCE = 1e-4;

    @Test
    void normalCdfAtZeroReturnsHalf() {
        assertEquals(0.5, Distributions.normalCdf(0.0), TOLERANCE);
    }

    @Test
    void normalCdfAt196ReturnsApprox975() {
        assertEquals(0.975, Distributions.normalCdf(1.96), TOLERANCE);
    }

    @Test
    void normalCdfAtNegative196ReturnsApprox025() {
        assertEquals(0.025, Distributions.normalCdf(-1.96), TOLERANCE);
    }

    @Test
    void normalCdfAtOneReturnsApprox841() {
        assertEquals(0.8413, Distributions.normalCdf(1.0), TOLERANCE);
    }

    @Test
    void normalCdfAtNegativeOneReturnsApprox159() {
        assertEquals(0.1587, Distributions.normalCdf(-1.0), TOLERANCE);
    }

    @Test
    void normalCdfAtPositiveInfinity() {
        assertEquals(1.0, Distributions.normalCdf(Double.POSITIVE_INFINITY), TOLERANCE);
    }

    @Test
    void normalCdfAtNegativeInfinity() {
        assertEquals(0.0, Distributions.normalCdf(Double.NEGATIVE_INFINITY), TOLERANCE);
    }

    @Test
    void normalInverseCdfAt975ReturnsApprox196() {
        assertEquals(1.96, Distributions.normalInverseCdf(0.975), 0.01);
    }

    @Test
    void normalInverseCdfAt50ReturnsZero() {
        assertEquals(0.0, Distributions.normalInverseCdf(0.5), TOLERANCE);
    }

    @Test
    void normalInverseCdfAt025ReturnsApproxNeg196() {
        assertEquals(-1.96, Distributions.normalInverseCdf(0.025), 0.01);
    }

    @Test
    void normalInverseCdfRoundTrip() {
        double[] testValues = {0.01, 0.05, 0.10, 0.25, 0.50, 0.75, 0.90, 0.95, 0.99};
        for (double p : testValues) {
            double z = Distributions.normalInverseCdf(p);
            double recovered = Distributions.normalCdf(z);
            assertEquals(p, recovered, TOLERANCE,
                    "Round-trip failed for p=" + p);
        }
    }

    @Test
    void normalInverseCdfRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Distributions.normalInverseCdf(0.0));
        assertThrows(IllegalArgumentException.class, () -> Distributions.normalInverseCdf(1.0));
        assertThrows(IllegalArgumentException.class, () -> Distributions.normalInverseCdf(-0.1));
        assertThrows(IllegalArgumentException.class, () -> Distributions.normalInverseCdf(1.1));
    }

    @Test
    void tCdfSymmetry() {
        // t distribution is symmetric: P(T <= -t) = 1 - P(T <= t)
        for (int df : new int[]{1, 5, 10, 30}) {
            double t = 2.0;
            double left = Distributions.tCdf(-t, df);
            double right = Distributions.tCdf(t, df);
            assertEquals(1.0, left + right, TOLERANCE,
                    "Symmetry failed for df=" + df);
        }
    }

    @Test
    void tCdfAtZeroReturnsHalf() {
        for (int df : new int[]{1, 5, 10, 30, 100}) {
            assertEquals(0.5, Distributions.tCdf(0.0, df), TOLERANCE,
                    "t CDF at 0 should be 0.5 for df=" + df);
        }
    }

    @Test
    void tCdfApproachesNormalForLargeDf() {
        // With large df, t-distribution approaches normal
        double t = 1.96;
        double tResult = Distributions.tCdf(t, 1000);
        double normalResult = Distributions.normalCdf(t);
        assertEquals(normalResult, tResult, 0.01,
                "t CDF should approach normal CDF for large df");
    }

    @Test
    void tCdfKnownValueDf10() {
        // P(T <= 2.228) for df=10 should be approximately 0.975
        assertEquals(0.975, Distributions.tCdf(2.228, 10), 0.01);
    }

    @Test
    void tCdfRejectsNonPositiveDf() {
        assertThrows(IllegalArgumentException.class, () -> Distributions.tCdf(1.0, 0));
        assertThrows(IllegalArgumentException.class, () -> Distributions.tCdf(1.0, -1));
    }

    @Test
    void tInverseCdfRoundTrip() {
        int[] dfs = {2, 5, 10, 30};
        double[] probs = {0.025, 0.05, 0.50, 0.90, 0.95, 0.975};
        for (int df : dfs) {
            for (double p : probs) {
                double t = Distributions.tInverseCdf(p, df);
                double recovered = Distributions.tCdf(t, df);
                assertEquals(p, recovered, 0.01,
                        "Round-trip failed for df=" + df + ", p=" + p);
            }
        }
    }

    @Test
    void regularizedBetaBoundaryValues() {
        assertEquals(0.0, Distributions.regularizedBeta(0.0, 1.0, 1.0), TOLERANCE);
        assertEquals(1.0, Distributions.regularizedBeta(1.0, 1.0, 1.0), TOLERANCE);
    }

    @Test
    void regularizedBetaUniformCase() {
        // For a=1, b=1: I_x(1,1) = x
        for (double x = 0.1; x <= 0.9; x += 0.1) {
            assertEquals(x, Distributions.regularizedBeta(x, 1.0, 1.0), TOLERANCE,
                    "I_x(1,1) should equal x for x=" + x);
        }
    }

    @Test
    void logGammaKnownValues() {
        // Gamma(1) = 1, so logGamma(1) = 0
        assertEquals(0.0, Distributions.logGamma(1.0), TOLERANCE);

        // Gamma(2) = 1, so logGamma(2) = 0
        assertEquals(0.0, Distributions.logGamma(2.0), TOLERANCE);

        // Gamma(5) = 24, so logGamma(5) = ln(24)
        assertEquals(Math.log(24.0), Distributions.logGamma(5.0), TOLERANCE);

        // Gamma(0.5) = sqrt(pi), so logGamma(0.5) = 0.5 * ln(pi)
        assertEquals(0.5 * Math.log(Math.PI), Distributions.logGamma(0.5), TOLERANCE);
    }

    @Test
    void logGammaRejectsNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> Distributions.logGamma(0.0));
        assertThrows(IllegalArgumentException.class, () -> Distributions.logGamma(-1.0));
    }

    @Test
    void tTwoTailPValueForZeroStatistic() {
        // t=0 should give p=1.0 (two-tailed)
        double pValue = Distributions.tTwoTailPValue(0.0, 10);
        assertEquals(1.0, pValue, TOLERANCE);
    }

    @Test
    void tTwoTailPValueForLargeStatistic() {
        // Very large t should give p close to 0
        double pValue = Distributions.tTwoTailPValue(100.0, 10);
        assertTrue(pValue < 0.001, "p-value for t=100 should be very small");
    }
}
