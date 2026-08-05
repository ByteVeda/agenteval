package org.byteveda.agenteval.statistics.math;

/**
 * Statistical distribution functions implemented from standard numerical approximations.
 *
 * <p>All methods are pure functions with no side effects, making this class thread-safe.</p>
 *
 * <p><strong>Internal API:</strong> This class is intended for use within the
 * agenteval-statistics module only. It is not part of the public API and may
 * change without notice.</p>
 */
public final class Distributions {

    private static final double SQRT_2PI = Math.sqrt(2.0 * Math.PI);
    private static final double LOG_SQRT_2PI = 0.5 * Math.log(2.0 * Math.PI);
    private static final int MAX_ITERATIONS = 200;
    private static final double EPSILON = 1e-10;

    private Distributions() {
        // utility class
    }

    /**
     * Standard normal CDF using Abramowitz and Stegun approximation (formula 26.2.17).
     *
     * @param z the z-score
     * @return P(Z &lt;= z) for standard normal Z
     */
    public static double normalCdf(double z) {
        if (Double.isNaN(z)) {
            return Double.NaN;
        }
        if (z == Double.POSITIVE_INFINITY) {
            return 1.0;
        }
        if (z == Double.NEGATIVE_INFINITY) {
            return 0.0;
        }

        // Use symmetry: for negative z, Phi(-z) = 1 - Phi(z)
        if (z < 0) {
            return 1.0 - normalCdf(-z);
        }

        // Abramowitz & Stegun 26.2.17
        double p = 0.2316419;
        double b1 = 0.319381530;
        double b2 = -0.356563782;
        double b3 = 1.781477937;
        double b4 = -1.821255978;
        double b5 = 1.330274429;

        double t = 1.0 / (1.0 + p * z);
        double t2 = t * t;
        double t3 = t2 * t;
        double t4 = t3 * t;
        double t5 = t4 * t;

        double pdf = Math.exp(-0.5 * z * z) / SQRT_2PI;
        double poly = b1 * t + b2 * t2 + b3 * t3 + b4 * t4 + b5 * t5;

        return 1.0 - pdf * poly;
    }

    /**
     * Inverse standard normal CDF using Beasley-Springer-Moro rational approximation.
     *
     * @param p the probability (0 &lt; p &lt; 1)
     * @return z such that P(Z &lt;= z) = p
     * @throws IllegalArgumentException if p is not in (0, 1)
     */
    public static double normalInverseCdf(double p) {
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("p must be in (0, 1), got: " + p);
        }

        // Beasley-Springer-Moro algorithm
        double[] a = {
                -3.969683028665376e+01,
                2.209460984245205e+02,
                -2.759285104469687e+02,
                1.383577518672690e+02,
                -3.066479806614716e+01,
                2.506628277459239e+00
        };
        double[] b = {
                -5.447609879822406e+01,
                1.615858368580409e+02,
                -1.556989798598866e+02,
                6.680131188771972e+01,
                -1.328068155288572e+01
        };
        double[] c = {
                -7.784894002430293e-03,
                -3.223964580411365e-01,
                -2.400758277161838e+00,
                -2.549732539343734e+00,
                4.374664141464968e+00,
                2.938163982698783e+00
        };
        double[] d = {
                7.784695709041462e-03,
                3.224671290700398e-01,
                2.445134137142996e+00,
                3.754408661907416e+00
        };

        double pLow = 0.02425;
        double pHigh = 1.0 - pLow;

        double result;

        if (p < pLow) {
            // Rational approximation for lower region
            double q = Math.sqrt(-2.0 * Math.log(p));
            result = (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        } else if (p <= pHigh) {
            // Rational approximation for central region
            double q = p - 0.5;
            double r = q * q;
            result = (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q
                    / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0);
        } else {
            // Rational approximation for upper region
            double q = Math.sqrt(-2.0 * Math.log(1.0 - p));
            result = -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
                    / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        }

        return result;
    }

    /**
     * Student's t distribution CDF using the regularized incomplete beta function.
     *
     * @param t the t-statistic
     * @param df degrees of freedom (must be positive)
     * @return P(T &lt;= t) for Student's t with df degrees of freedom
     */
    public static double tCdf(double t, int df) {
        if (df <= 0) {
            throw new IllegalArgumentException("degrees of freedom must be positive, got: " + df);
        }
        double x = df / (df + t * t);
        double beta = 0.5 * regularizedBeta(x, 0.5 * df, 0.5);
        return t >= 0 ? 1.0 - beta : beta;
    }

    /**
     * Inverse Student's t CDF using Newton-Raphson iteration.
     *
     * @param p the probability (0 &lt; p &lt; 1)
     * @param df degrees of freedom (must be positive)
     * @return t such that P(T &lt;= t) = p
     */
    public static double tInverseCdf(double p, int df) {
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("p must be in (0, 1), got: " + p);
        }
        if (df <= 0) {
            throw new IllegalArgumentException("degrees of freedom must be positive, got: " + df);
        }

        // Initial guess from normal approximation
        double t = normalInverseCdf(p);

        // Newton-Raphson refinement
        for (int i = 0; i < 50; i++) {
            double cdf = tCdf(t, df);
            double pdf = tPdf(t, df);
            if (Math.abs(pdf) < 1e-15) {
                break;
            }
            double delta = (cdf - p) / pdf;
            t -= delta;
            if (Math.abs(delta) < 1e-12) {
                break;
            }
        }

        return t;
    }

    /**
     * Student's t probability density function.
     */
    private static double tPdf(double t, int df) {
        double halfDfPlus1 = 0.5 * (df + 1);
        double halfDf = 0.5 * df;
        return Math.exp(logGamma(halfDfPlus1) - logGamma(halfDf)
                - 0.5 * Math.log(df * Math.PI)
                - halfDfPlus1 * Math.log(1.0 + t * t / df));
    }

    /**
     * Regularized incomplete beta function I_x(a,b) using Lentz's continued fraction algorithm.
     *
     * @param x the integration limit (0 &lt;= x &lt;= 1)
     * @param a shape parameter (positive)
     * @param b shape parameter (positive)
     * @return I_x(a, b)
     */
    public static double regularizedBeta(double x, double a, double b) {
        if (x < 0.0 || x > 1.0) {
            throw new IllegalArgumentException("x must be in [0, 1], got: " + x);
        }
        if (x == 0.0) {
            return 0.0;
        }
        if (x == 1.0) {
            return 1.0;
        }

        // Use symmetry relation for better convergence
        if (x > (a + 1.0) / (a + b + 2.0)) {
            return 1.0 - regularizedBeta(1.0 - x, b, a);
        }

        double logPrefix = a * Math.log(x) + b * Math.log(1.0 - x)
                - Math.log(a) - logBeta(a, b);

        return Math.exp(logPrefix) * betaContinuedFraction(x, a, b);
    }

    /**
     * Continued fraction for the incomplete beta function using Lentz's algorithm.
     */
    private static double betaContinuedFraction(double x, double a, double b) {
        double tiny = 1e-30;
        double f = 1.0;
        double c = 1.0;
        double d = 1.0 - (a + b) * x / (a + 1.0);
        if (Math.abs(d) < tiny) {
            d = tiny;
        }
        d = 1.0 / d;
        f = d;

        for (int m = 1; m <= MAX_ITERATIONS; m++) {
            // Even step
            int m2 = 2 * m;
            double numerator = m * (b - m) * x / ((a + m2 - 1.0) * (a + m2));
            d = 1.0 + numerator * d;
            if (Math.abs(d) < tiny) {
                d = tiny;
            }
            c = 1.0 + numerator / c;
            if (Math.abs(c) < tiny) {
                c = tiny;
            }
            d = 1.0 / d;
            f *= c * d;

            // Odd step
            numerator = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1.0));
            d = 1.0 + numerator * d;
            if (Math.abs(d) < tiny) {
                d = tiny;
            }
            c = 1.0 + numerator / c;
            if (Math.abs(c) < tiny) {
                c = tiny;
            }
            d = 1.0 / d;
            double delta = c * d;
            f *= delta;

            if (Math.abs(delta - 1.0) < EPSILON) {
                return f;
            }
        }

        return f;
    }

    /**
     * Log of the beta function: log(B(a, b)) = logGamma(a) + logGamma(b) - logGamma(a + b).
     */
    private static double logBeta(double a, double b) {
        return logGamma(a) + logGamma(b) - logGamma(a + b);
    }

    /**
     * Log-gamma function using the Lanczos approximation (g=7, n=9 coefficients).
     *
     * @param x the argument (must be positive)
     * @return ln(Gamma(x))
     */
    public static double logGamma(double x) {
        if (x <= 0) {
            throw new IllegalArgumentException("x must be positive, got: " + x);
        }

        double[] coefficients = {
            0.99999999999980993,
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
        };

        if (x < 0.5) {
            // Reflection formula: Gamma(x)*Gamma(1-x) = pi/sin(pi*x)
            return Math.log(Math.PI / Math.sin(Math.PI * x)) - logGamma(1.0 - x);
        }

        x -= 1.0;
        double a = coefficients[0];
        double t = x + 7.5;

        for (int i = 1; i < coefficients.length; i++) {
            a += coefficients[i] / (x + i);
        }

        return LOG_SQRT_2PI + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }

    /**
     * Computes the two-tailed p-value for a t-statistic.
     *
     * @param t the t-statistic
     * @param df degrees of freedom
     * @return two-tailed p-value
     */
    public static double tTwoTailPValue(double t, int df) {
        return 2.0 * (1.0 - tCdf(Math.abs(t), df));
    }
}
