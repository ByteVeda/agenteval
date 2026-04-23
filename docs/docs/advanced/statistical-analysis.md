---
sidebar_position: 6
---

# Statistical Analysis

The `agenteval-statistics` module adds statistical rigor to evaluation results. It computes descriptive statistics, confidence intervals, significance tests, and stability analysis so you can answer questions like "Is this score change real or just noise?"

## Dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-statistics</artifactId>
  <version>0.2.0</version>
  <scope>test</scope>
</dependency>
```

## Single-Run Analysis

Analyze a single `EvalResult` to get per-metric and overall descriptive statistics, confidence intervals, and normality tests:

```java
EvalResult result = AgentEval.evaluate(testCases, metrics);

StatisticalReport report = StatisticalAnalyzer.analyze(result);
```

The returned `StatisticalReport` contains:

- `metricStatistics()` -- a `Map<String, MetricStatistics>` with per-metric descriptive statistics, confidence intervals, and normality tests
- `overallDescriptive()` -- `DescriptiveStatistics` across all scores (mean, stdDev, median, skewness, kurtosis, CV, etc.)
- `overallConfidenceInterval()` -- a `ConfidenceInterval` for the overall mean
- `warnings()` -- a list of statistical warnings (e.g., high variance, small sample size)

Confidence intervals require at least 2 observations. Normality tests (Jarque-Bera approximation) require at least 8 observations. Warnings are emitted when sample sizes are too small or when the coefficient of variation exceeds the configured threshold.

## Two-Run Comparison

Compare a baseline and current evaluation run to determine whether score changes are statistically significant:

```java
EvalResult baseline = AgentEval.evaluate(testCases, metrics);
// ... make changes to the agent ...
EvalResult current = AgentEval.evaluate(testCases, metrics);

EnhancedRegressionReport report = StatisticalAnalyzer.compare(baseline, current);

// Is the overall difference statistically significant?
boolean significant = report.isSignificant();

// Are there statistically significant regressions?
boolean regressions = report.hasSignificantRegressions();

// Per-metric comparisons
for (var entry : report.metricComparisons().entrySet()) {
    StatisticalComparison cmp = entry.getValue();
    System.out.printf("%s: delta=%.3f significant=%s effect=%s%n",
        entry.getKey(), cmp.delta(),
        cmp.significanceTest().significant(),
        cmp.effectSize().magnitude());
}
```

The comparison uses a paired t-test for significance testing and Cohen's d for effect size measurement. Both the baseline and current runs must have equal sample sizes (at least 2) for the paired t-test to be valid.

The `EnhancedRegressionReport` wraps the base `RegressionReport` with:

- `overallSignificance()` -- a `SignificanceTest` with p-value and significance flag
- `overallEffectSize()` -- an `EffectSize` with magnitude (`NEGLIGIBLE`, `SMALL`, `MEDIUM`, `LARGE`)
- `metricComparisons()` -- per-metric `StatisticalComparison` records with delta, significance test, and effect size

## Multi-Run Stability

When you run the same evaluation multiple times (e.g., to assess LLM non-determinism), use `analyzeStability()` to check consistency:

```java
List<EvalResult> runs = new ArrayList<>();
for (int i = 0; i < 5; i++) {
    runs.add(AgentEval.evaluate(testCases, metrics));
}

StabilityAnalysis stability = StatisticalAnalyzer.analyzeStability(runs);

// Overall consistency
RunConsistency overall = stability.overallConsistency();
System.out.printf("Overall: mean=%.3f stdDev=%.3f CV=%.3f stable=%s (%s)%n",
    overall.meanScore(), overall.standardDeviation(),
    overall.coefficientOfVariation(), overall.isStable(),
    overall.assessment());

// Per-metric consistency
for (var entry : stability.metricConsistency().entrySet()) {
    RunConsistency rc = entry.getValue();
    System.out.printf("%s: %s (CV=%.3f)%n",
        entry.getKey(), rc.assessment(), rc.coefficientOfVariation());
}
```

Stability assessments are based on the coefficient of variation (CV):

| CV Range | Assessment |
|---|---|
| CV ≤ 0.05 | Highly stable |
| CV ≤ threshold | Stable |
| CV ≤ threshold x 2 | Moderately unstable |
| CV &gt; threshold x 2 | Highly unstable |

A warning is emitted if fewer than 3 runs are provided.

## Configuration

Use `StatisticalConfig` to customize analysis parameters:

```java
var config = StatisticalConfig.builder()
    .confidenceLevel(ConfidenceLevel.P99)
    .significanceAlpha(0.01)
    .cvThreshold(0.10)
    .bootstrapIterations(20_000)
    .desiredPower(0.90)
    .build();

StatisticalReport report = StatisticalAnalyzer.analyze(result, config);
EnhancedRegressionReport comparison = StatisticalAnalyzer.compare(baseline, current, config);
StabilityAnalysis stability = StatisticalAnalyzer.analyzeStability(runs, config);
```

| Parameter | Default | Description |
|---|---|---|
| `confidenceLevel` | `ConfidenceLevel.P95` | Confidence level for intervals (`P90`, `P95`, `P99`) |
| `significanceAlpha` | `0.05` | Alpha level for significance tests |
| `cvThreshold` | `0.15` | Coefficient of variation threshold for high-variance flagging |
| `bootstrapIterations` | `10,000` | Number of bootstrap iterations |
| `desiredPower` | `0.80` | Desired statistical power (1 - beta) |

Calling `StatisticalConfig.defaults()` returns a config with all default values.

## Statistical Methods Used

| Method | Purpose |
|---|---|
| Descriptive statistics | Mean, median, standard deviation, skewness, kurtosis, CV |
| Student's t confidence interval | Confidence interval for the mean score |
| Paired t-test | Significance testing between two evaluation runs |
| Cohen's d | Effect size measurement for comparisons |
| Jarque-Bera (approximate) | Normality testing using skewness and kurtosis |
| Coefficient of variation | Stability assessment across multiple runs |
