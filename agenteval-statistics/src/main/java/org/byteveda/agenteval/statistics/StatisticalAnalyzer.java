package org.byteveda.agenteval.statistics;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.reporting.regression.RegressionComparison;
import org.byteveda.agenteval.reporting.regression.RegressionReport;
import org.byteveda.agenteval.statistics.comparison.EnhancedRegressionReport;
import org.byteveda.agenteval.statistics.comparison.StatisticalComparison;
import org.byteveda.agenteval.statistics.descriptive.DescriptiveCalculator;
import org.byteveda.agenteval.statistics.descriptive.DescriptiveStatistics;
import org.byteveda.agenteval.statistics.inference.ConfidenceInterval;
import org.byteveda.agenteval.statistics.inference.EffectSize;
import org.byteveda.agenteval.statistics.inference.InferenceCalculator;
import org.byteveda.agenteval.statistics.inference.NormalityTest;
import org.byteveda.agenteval.statistics.inference.SampleSizeRecommendation;
import org.byteveda.agenteval.statistics.inference.SignificanceTest;
import org.byteveda.agenteval.statistics.report.MetricStatistics;
import org.byteveda.agenteval.statistics.report.StatisticalReport;
import org.byteveda.agenteval.statistics.stability.RunConsistency;
import org.byteveda.agenteval.statistics.stability.StabilityAnalysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Main facade for statistical analysis of evaluation results.
 *
 * <p>All methods are static and thread-safe. Use {@link StatisticalConfig} to
 * customize analysis parameters.</p>
 */
public final class StatisticalAnalyzer {

    private static final int MIN_SAMPLE_FOR_NORMALITY = 8;

    private StatisticalAnalyzer() {
        // utility class
    }

    /**
     * Analyzes a single evaluation result with default configuration.
     *
     * @param result the evaluation result to analyze
     * @return a statistical report
     */
    public static StatisticalReport analyze(EvalResult result) {
        return analyze(result, StatisticalConfig.defaults());
    }

    /**
     * Analyzes a single evaluation result with the given configuration.
     *
     * @param result the evaluation result to analyze
     * @param config the statistical configuration
     * @return a statistical report
     */
    public static StatisticalReport analyze(EvalResult result, StatisticalConfig config) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(config, "config must not be null");

        List<String> warnings = new ArrayList<>();
        Map<String, MetricStatistics> metricStats = new LinkedHashMap<>();

        // Collect per-metric score arrays
        Map<String, List<Double>> scoresByMetric = collectScoresByMetric(result);

        for (Map.Entry<String, List<Double>> entry : scoresByMetric.entrySet()) {
            String metricName = entry.getKey();
            double[] values = toDoubleArray(entry.getValue());

            DescriptiveStatistics descriptive = DescriptiveCalculator.compute(
                    metricName, values, config.cvThreshold());

            if (descriptive.highVarianceFlag()) {
                warnings.add(String.format("Metric '%s' has high variance (CV=%.3f > %.3f)",
                        metricName, descriptive.coefficientOfVariation(), config.cvThreshold()));
            }

            ConfidenceInterval ci = null;
            if (values.length >= 2) {
                ci = InferenceCalculator.tConfidenceInterval(values, config.confidenceLevel());
            } else {
                warnings.add(String.format(
                        "Metric '%s' has fewer than 2 observations; "
                        + "confidence interval cannot be computed", metricName));
            }

            NormalityTest normality = null;
            if (values.length >= MIN_SAMPLE_FOR_NORMALITY) {
                normality = approximateNormalityTest(metricName, descriptive, config);
            }

            metricStats.put(metricName, new MetricStatistics(metricName, descriptive, ci,
                    normality));
        }

        // Overall statistics
        double[] allScores = collectAllScores(result);
        DescriptiveStatistics overallDescriptive = null;
        ConfidenceInterval overallCi = null;
        SampleSizeRecommendation sampleSizeRec = null;

        if (allScores.length > 0) {
            overallDescriptive = DescriptiveCalculator.compute("overall", allScores,
                    config.cvThreshold());
            if (allScores.length >= 2) {
                overallCi = InferenceCalculator.tConfidenceInterval(allScores,
                        config.confidenceLevel());
            }
        }

        if (allScores.length < 30) {
            warnings.add(String.format(
                    "Sample size (%d) is small; consider running more test cases "
                    + "for reliable statistical inference", allScores.length));
        }

        return new StatisticalReport(metricStats, overallDescriptive, overallCi,
                warnings, sampleSizeRec);
    }

    /**
     * Compares baseline and current evaluation results with default configuration.
     *
     * @param baseline the baseline evaluation result
     * @param current the current evaluation result
     * @return an enhanced regression report with statistical significance
     */
    public static EnhancedRegressionReport compare(EvalResult baseline, EvalResult current) {
        return compare(baseline, current, StatisticalConfig.defaults());
    }

    /**
     * Compares baseline and current evaluation results with the given configuration.
     *
     * @param baseline the baseline evaluation result
     * @param current the current evaluation result
     * @param config the statistical configuration
     * @return an enhanced regression report with statistical significance
     */
    public static EnhancedRegressionReport compare(EvalResult baseline, EvalResult current,
            StatisticalConfig config) {
        Objects.requireNonNull(baseline, "baseline must not be null");
        Objects.requireNonNull(current, "current must not be null");
        Objects.requireNonNull(config, "config must not be null");

        RegressionReport baseReport = RegressionComparison.compare(baseline, current);
        List<String> warnings = new ArrayList<>();

        // Overall scores
        double[] baselineScores = collectAllScores(baseline);
        double[] currentScores = collectAllScores(current);

        SignificanceTest overallSig;
        EffectSize overallEffect;

        if (baselineScores.length >= 2 && currentScores.length >= 2
                && baselineScores.length == currentScores.length) {
            overallSig = InferenceCalculator.pairedTTest(baselineScores, currentScores,
                    config.significanceAlpha());
            overallEffect = InferenceCalculator.cohensD(baselineScores, currentScores);
        } else {
            warnings.add("Cannot perform paired t-test: arrays must have equal length >= 2. "
                    + "Baseline: " + baselineScores.length + ", Current: " + currentScores.length);
            overallSig = new SignificanceTest("Paired t-test", Double.NaN, Double.NaN,
                    false, config.significanceAlpha(),
                    "Insufficient or mismatched data for significance testing");
            overallEffect = new EffectSize(0.0, EffectSize.Magnitude.NEGLIGIBLE);
        }

        // Per-metric comparisons
        Map<String, StatisticalComparison> metricComparisons = new LinkedHashMap<>();
        Map<String, List<Double>> baselineByMetric = collectScoresByMetric(baseline);
        Map<String, List<Double>> currentByMetric = collectScoresByMetric(current);

        Set<String> allMetrics = new LinkedHashSet<>(baselineByMetric.keySet());
        allMetrics.addAll(currentByMetric.keySet());

        for (String metric : allMetrics) {
            List<Double> baseScores = baselineByMetric.get(metric);
            List<Double> curScores = currentByMetric.get(metric);

            if (baseScores == null || curScores == null) {
                warnings.add(String.format("Metric '%s' not present in both runs; "
                        + "skipping comparison", metric));
                continue;
            }

            double[] baseArr = toDoubleArray(baseScores);
            double[] curArr = toDoubleArray(curScores);

            DescriptiveStatistics baseStats = DescriptiveCalculator.compute(metric, baseArr,
                    config.cvThreshold());
            DescriptiveStatistics curStats = DescriptiveCalculator.compute(metric, curArr,
                    config.cvThreshold());
            double delta = curStats.mean() - baseStats.mean();

            SignificanceTest sigTest;
            EffectSize effectSize;

            if (baseArr.length >= 2 && curArr.length >= 2
                    && baseArr.length == curArr.length) {
                sigTest = InferenceCalculator.pairedTTest(baseArr, curArr,
                        config.significanceAlpha());
                effectSize = InferenceCalculator.cohensD(baseArr, curArr);
            } else {
                sigTest = new SignificanceTest("Paired t-test", Double.NaN, Double.NaN,
                        false, config.significanceAlpha(),
                        "Insufficient or mismatched data");
                effectSize = new EffectSize(0.0, EffectSize.Magnitude.NEGLIGIBLE);
            }

            metricComparisons.put(metric, new StatisticalComparison(
                    metric, baseStats, curStats, delta, sigTest, effectSize));
        }

        return new EnhancedRegressionReport(baseReport, overallSig, overallEffect,
                metricComparisons, warnings);
    }

    /**
     * Analyzes stability across multiple evaluation runs with default configuration.
     *
     * @param runs the list of evaluation runs
     * @return the stability analysis
     */
    public static StabilityAnalysis analyzeStability(List<EvalResult> runs) {
        return analyzeStability(runs, StatisticalConfig.defaults());
    }

    /**
     * Analyzes stability across multiple evaluation runs with the given configuration.
     *
     * @param runs the list of evaluation runs
     * @param config the statistical configuration
     * @return the stability analysis
     */
    public static StabilityAnalysis analyzeStability(List<EvalResult> runs,
            StatisticalConfig config) {
        Objects.requireNonNull(runs, "runs must not be null");
        Objects.requireNonNull(config, "config must not be null");

        if (runs.isEmpty()) {
            throw new IllegalArgumentException("runs must not be empty");
        }

        List<String> warnings = new ArrayList<>();
        int numberOfRuns = runs.size();

        if (numberOfRuns < 3) {
            warnings.add("Fewer than 3 runs; stability assessment may be unreliable");
        }

        // Per-metric consistency: collect per-run average scores for each metric
        Map<String, List<Double>> metricRunScores = new LinkedHashMap<>();

        for (EvalResult run : runs) {
            Map<String, Double> avgByMetric = run.averageScoresByMetric();
            for (Map.Entry<String, Double> entry : avgByMetric.entrySet()) {
                metricRunScores.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }

        Map<String, RunConsistency> metricConsistency = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : metricRunScores.entrySet()) {
            String metricName = entry.getKey();
            double[] values = toDoubleArray(entry.getValue());

            RunConsistency consistency = computeConsistency(metricName, values,
                    numberOfRuns, config.cvThreshold());
            metricConsistency.put(metricName, consistency);

            if (!consistency.isStable()) {
                warnings.add(String.format("Metric '%s' shows instability (CV=%.3f > %.3f)",
                        metricName, consistency.coefficientOfVariation(), config.cvThreshold()));
            }
        }

        // Overall consistency using per-run average scores
        double[] overallRunScores = new double[numberOfRuns];
        for (int i = 0; i < numberOfRuns; i++) {
            overallRunScores[i] = runs.get(i).averageScore();
        }

        RunConsistency overallConsistency = computeConsistency("overall", overallRunScores,
                numberOfRuns, config.cvThreshold());

        return new StabilityAnalysis(numberOfRuns, metricConsistency, overallConsistency, warnings);
    }

    // --- Internal helpers ---

    private static Map<String, List<Double>> collectScoresByMetric(EvalResult result) {
        Map<String, List<Double>> scoresByMetric = new LinkedHashMap<>();
        for (CaseResult cr : result.caseResults()) {
            for (Map.Entry<String, EvalScore> entry : cr.scores().entrySet()) {
                scoresByMetric.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                        .add(entry.getValue().value());
            }
        }
        return scoresByMetric;
    }

    private static double[] collectAllScores(EvalResult result) {
        List<Double> allScores = new ArrayList<>();
        for (CaseResult cr : result.caseResults()) {
            for (EvalScore score : cr.scores().values()) {
                allScores.add(score.value());
            }
        }
        return toDoubleArray(allScores);
    }

    private static double[] toDoubleArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static RunConsistency computeConsistency(String metricName, double[] values,
            int numberOfRuns, double cvThreshold) {
        double mean = 0.0;
        for (double v : values) {
            mean += v;
        }
        mean /= values.length;

        double variance = 0.0;
        if (values.length > 1) {
            for (double v : values) {
                double diff = v - mean;
                variance += diff * diff;
            }
            variance /= (values.length - 1);
        }
        double stdDev = Math.sqrt(variance);
        double cv = mean == 0.0 ? 0.0 : Math.abs(stdDev / mean);
        boolean isStable = cv <= cvThreshold;

        String assessment;
        if (cv <= 0.05) {
            assessment = "Highly stable";
        } else if (cv <= cvThreshold) {
            assessment = "Stable";
        } else if (cv <= cvThreshold * 2) {
            assessment = "Moderately unstable";
        } else {
            assessment = "Highly unstable";
        }

        return new RunConsistency(metricName, numberOfRuns, mean, stdDev, cv,
                isStable, assessment);
    }

    /**
     * Approximate normality test using skewness and kurtosis (Jarque-Bera-like heuristic).
     * Uses the D'Agostino-Pearson criterion: data is approximately normal if
     * |skewness| &lt; 2 and |kurtosis| &lt; 7.
     */
    private static NormalityTest approximateNormalityTest(String metricName,
            DescriptiveStatistics stats,
            StatisticalConfig config) {
        int n = stats.n();
        double skewness = stats.skewness();
        double kurtosis = stats.kurtosis();

        // Jarque-Bera test statistic
        double jb = (n / 6.0) * (skewness * skewness
                + (kurtosis * kurtosis) / 4.0);

        // Approximate p-value using chi-squared(2) distribution
        // For chi-squared with df=2, CDF = 1 - exp(-x/2)
        double pValue = Math.exp(-jb / 2.0);

        boolean isNormal = pValue >= config.significanceAlpha();

        return new NormalityTest(metricName, jb, pValue, isNormal,
                "Jarque-Bera (approximate)");
    }
}
