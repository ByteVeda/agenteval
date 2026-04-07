package org.byteveda.agenteval.statistics.inference;

import org.byteveda.agenteval.statistics.math.Distributions;
import org.byteveda.agenteval.statistics.math.BootstrapSampler;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/**
 * Static utility for inferential statistics: confidence intervals, significance tests,
 * effect sizes, and sample size recommendations.
 *
 * <p>All methods are stateless and thread-safe.</p>
 */
public final class InferenceCalculator {

    private static final int DEFAULT_BOOTSTRAP_ITERATIONS = 10_000;
    private static final long DEFAULT_SEED = 42L;

    private InferenceCalculator() {
        // utility class
    }

    /**
     * Computes a confidence interval for the mean using Student's t-distribution.
     *
     * @param values the sample values (at least 2 elements)
     * @param level the desired confidence level
     * @return the confidence interval
     * @throws IllegalArgumentException if values has fewer than 2 elements
     */
    public static ConfidenceInterval tConfidenceInterval(double[] values,
            ConfidenceLevel level) {
        if (values.length < 2) {
            throw new IllegalArgumentException(
                    "t confidence interval requires at least 2 values, got: " + values.length);
        }

        int n = values.length;
        double mean = mean(values);
        double stdDev = stdDev(values, mean);
        int df = n - 1;
        double alpha = 1.0 - level.level();
        double tCritical = Distributions.tInverseCdf(1.0 - alpha / 2.0, df);
        double marginOfError = tCritical * stdDev / Math.sqrt(n);

        return new ConfidenceInterval(
                mean - marginOfError,
                mean + marginOfError,
                level.level(),
                mean,
                "t-distribution"
        );
    }

    /**
     * Computes a bootstrap percentile confidence interval for the mean.
     *
     * @param values the sample values (at least 1 element)
     * @param level the desired confidence level
     * @param iterations the number of bootstrap iterations
     * @return the confidence interval
     * @throws IllegalArgumentException if values is empty or iterations is non-positive
     */
    public static ConfidenceInterval bootstrapConfidenceInterval(double[] values,
            ConfidenceLevel level,
            int iterations) {
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }

        RandomGenerator rng = RandomGenerator.of("L64X128MixRandom");
        // Use a deterministic splittable generator seeded for reproducibility
        double[] means = BootstrapSampler.bootstrapMeans(values, iterations, rng);

        double alpha = 1.0 - level.level();
        int lowerIdx = Math.max(0, (int) Math.floor(alpha / 2.0 * iterations) - 1);
        int upperIdx = Math.min(iterations - 1, (int) Math.ceil((1.0 - alpha / 2.0) * iterations) - 1);

        double mean = mean(values);

        return new ConfidenceInterval(
                means[lowerIdx],
                means[upperIdx],
                level.level(),
                mean,
                "bootstrap-percentile"
        );
    }

    /**
     * Performs a paired t-test comparing two matched samples.
     *
     * @param baseline the baseline scores
     * @param current the current scores
     * @param alpha the significance level
     * @return the significance test result
     * @throws IllegalArgumentException if arrays have different lengths or fewer than 2 elements
     */
    public static SignificanceTest pairedTTest(double[] baseline, double[] current, double alpha) {
        validatePairedArrays(baseline, current);

        int n = baseline.length;
        double[] diffs = new double[n];
        for (int i = 0; i < n; i++) {
            diffs[i] = current[i] - baseline[i];
        }

        double meanDiff = mean(diffs);
        double stdDiff = stdDev(diffs, meanDiff);

        double tStat;
        double pValue;

        if (stdDiff == 0.0) {
            // All differences are identical
            tStat = meanDiff == 0.0 ? 0.0 : Double.POSITIVE_INFINITY;
            pValue = meanDiff == 0.0 ? 1.0 : 0.0;
        } else {
            tStat = meanDiff / (stdDiff / Math.sqrt(n));
            int df = n - 1;
            pValue = Distributions.tTwoTailPValue(tStat, df);
        }

        boolean significant = pValue < alpha;
        String interpretation = significant
                ? String.format("Significant difference detected (p=%.4f < alpha=%.4f). "
                        + "Mean difference: %.4f", pValue, alpha, meanDiff)
                : String.format("No significant difference (p=%.4f >= alpha=%.4f). "
                        + "Mean difference: %.4f", pValue, alpha, meanDiff);

        return new SignificanceTest("Paired t-test", tStat, pValue, significant,
                alpha, interpretation);
    }

    /**
     * Performs a Wilcoxon signed-rank test comparing two matched samples.
     * Uses normal approximation with continuity correction for n &gt;= 10.
     *
     * @param baseline the baseline scores
     * @param current the current scores
     * @param alpha the significance level
     * @return the significance test result
     * @throws IllegalArgumentException if arrays have different lengths or fewer than 10 elements
     */
    public static SignificanceTest wilcoxonSignedRank(double[] baseline, double[] current,
            double alpha) {
        validatePairedArrays(baseline, current);
        if (baseline.length < 10) {
            throw new IllegalArgumentException(
                    "Wilcoxon signed-rank test requires at least 10 paired observations "
                    + "for normal approximation, got: " + baseline.length);
        }

        int n = baseline.length;
        double[] diffs = new double[n];
        int nonZeroCount = 0;

        for (int i = 0; i < n; i++) {
            double diff = current[i] - baseline[i];
            if (diff != 0.0) {
                diffs[nonZeroCount++] = diff;
            }
        }

        if (nonZeroCount == 0) {
            return new SignificanceTest("Wilcoxon signed-rank test", 0.0, 1.0, false,
                    alpha, "All differences are zero; no significant difference.");
        }

        // Rank absolute differences
        double[] absDiffs = new double[nonZeroCount];
        for (int i = 0; i < nonZeroCount; i++) {
            absDiffs[i] = Math.abs(diffs[i]);
        }

        int[] indices = rankIndices(absDiffs, nonZeroCount);
        double[] ranks = computeRanks(absDiffs, indices, nonZeroCount);

        // Sum ranks of positive differences
        double wPlus = 0.0;
        for (int i = 0; i < nonZeroCount; i++) {
            if (diffs[i] > 0.0) {
                wPlus += ranks[i];
            }
        }

        // Normal approximation with continuity correction
        double nEff = nonZeroCount;
        double expectedW = nEff * (nEff + 1.0) / 4.0;
        double varW = nEff * (nEff + 1.0) * (2.0 * nEff + 1.0) / 24.0;
        double z = (Math.abs(wPlus - expectedW) - 0.5) / Math.sqrt(varW);
        double pValue = 2.0 * (1.0 - Distributions.normalCdf(Math.abs(z)));

        boolean significant = pValue < alpha;
        String interpretation = significant
                ? String.format("Significant difference detected (p=%.4f < alpha=%.4f, W+=%.1f)",
                        pValue, alpha, wPlus)
                : String.format("No significant difference (p=%.4f >= alpha=%.4f, W+=%.1f)",
                        pValue, alpha, wPlus);

        return new SignificanceTest("Wilcoxon signed-rank test", wPlus, pValue, significant,
                alpha, interpretation);
    }

    /**
     * Computes Cohen's d effect size for two independent or paired samples.
     * Uses pooled standard deviation.
     *
     * @param baseline the baseline scores
     * @param current the current scores
     * @return the effect size result
     * @throws IllegalArgumentException if arrays have different lengths or fewer than 2 elements
     */
    public static EffectSize cohensD(double[] baseline, double[] current) {
        validatePairedArrays(baseline, current);

        double meanBaseline = mean(baseline);
        double meanCurrent = mean(current);
        double varBaseline = variance(baseline, meanBaseline);
        double varCurrent = variance(current, meanCurrent);

        // Pooled standard deviation
        double pooledVar = (varBaseline + varCurrent) / 2.0;
        double pooledStdDev = Math.sqrt(pooledVar);

        double d = pooledStdDev == 0.0 ? 0.0 : (meanCurrent - meanBaseline) / pooledStdDev;
        EffectSize.Magnitude magnitude = EffectSize.classify(d);

        return new EffectSize(d, magnitude);
    }

    /**
     * Recommends a sample size for a two-sample t-test given the observed effect size.
     * Uses the formula: n = ((z_alpha/2 + z_beta) / d)^2 per group.
     *
     * @param observedEffectSize the observed Cohen's d
     * @param alpha the desired significance level
     * @param power the desired power (1 - beta)
     * @return the sample size recommendation
     */
    public static SampleSizeRecommendation recommendSampleSize(double observedEffectSize,
            double alpha, double power) {
        double effectSize = Math.abs(observedEffectSize);
        int recommended;
        String rationale;

        if (effectSize < 0.01) {
            recommended = 1000;
            rationale = String.format(
                    "Effect size is negligible (d=%.4f). At least %d samples per group "
                    + "recommended, but the practical significance of such a small effect "
                    + "should be questioned.", effectSize, recommended);
        } else {
            double zAlpha = Distributions.normalInverseCdf(1.0 - alpha / 2.0);
            double zBeta = Distributions.normalInverseCdf(power);
            double nPerGroup = Math.pow((zAlpha + zBeta) / effectSize, 2);
            recommended = (int) Math.ceil(nPerGroup);
            rationale = String.format(
                    "For effect size d=%.4f, alpha=%.4f, power=%.4f: "
                    + "need %d samples per group to detect this effect reliably.",
                    effectSize, alpha, power, recommended);
        }

        return new SampleSizeRecommendation(0, recommended, alpha, power,
                observedEffectSize, rationale);
    }

    // --- Internal helpers ---

    private static void validatePairedArrays(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Arrays must have the same length, got: " + a.length + " and " + b.length);
        }
        if (a.length < 2) {
            throw new IllegalArgumentException(
                    "Arrays must have at least 2 elements, got: " + a.length);
        }
    }

    private static double mean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private static double variance(double[] values, double mean) {
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

    private static double stdDev(double[] values, double mean) {
        return Math.sqrt(variance(values, mean));
    }

    /**
     * Returns indices that sort the array in ascending order.
     */
    private static int[] rankIndices(double[] values, int count) {
        Integer[] indices = new Integer[count];
        for (int i = 0; i < count; i++) {
            indices[i] = i;
        }
        Arrays.sort(indices, (a, b) -> Double.compare(values[a], values[b]));
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = indices[i];
        }
        return result;
    }

    /**
     * Computes ranks with tie handling (average ranks for ties).
     */
    private static double[] computeRanks(double[] values, int[] sortedIndices, int count) {
        double[] ranks = new double[count];
        int i = 0;
        while (i < count) {
            int j = i;
            // Find ties
            while (j < count - 1
                    && values[sortedIndices[j]] == values[sortedIndices[j + 1]]) {
                j++;
            }
            // Average rank for tied values
            double avgRank = (i + j) / 2.0 + 1.0;
            for (int k = i; k <= j; k++) {
                ranks[sortedIndices[k]] = avgRank;
            }
            i = j + 1;
        }
        return ranks;
    }
}
