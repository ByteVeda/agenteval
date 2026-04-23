---
sidebar_position: 9
---

# Capability Fingerprinting

The `agenteval-fingerprint` module profiles an agent's capabilities across multiple dimensions, producing a structured "fingerprint" that can be compared across agent versions, models, or configurations.

## Dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-fingerprint</artifactId>
  <version>0.2.0</version>
  <scope>test</scope>
</dependency>
```

## Profiling an Agent

Use `CapabilityProfiler` to run targeted benchmarks for each capability dimension and aggregate the results into a `CapabilityProfile`:

```java
var profile = CapabilityProfiler.builder()
    .agentName("my-agent-v2")
    .addBenchmark(new DimensionBenchmark(
        CapabilityDimension.ACCURACY,
        List.of(new CorrectnessMetric(judge, 0.7)),
        accuracyTestCases
    ))
    .addBenchmark(new DimensionBenchmark(
        CapabilityDimension.SAFETY,
        List.of(new Toxicity(0.5)),
        safetyTestCases
    ))
    .build()
    .profile();

// Overall score (average across all dimensions)
double overall = profile.overallScore();

// Dimensions scoring >= 0.8
List<CapabilityDimension> strengths = profile.strengths();

// Dimensions scoring < 0.5
List<CapabilityDimension> weaknesses = profile.weaknesses();
```

You can also use custom thresholds for strengths and weaknesses:

```java
List<CapabilityDimension> strong = profile.strengths(0.9);
List<CapabilityDimension> weak = profile.weaknesses(0.6);
```

## Capability Dimensions

Eight dimensions are defined in the `CapabilityDimension` enum:

| Dimension | Description |
|---|---|
| `ACCURACY` | Correctness and factual precision of agent responses |
| `RELEVANCY` | How well the agent's responses address the user's query |
| `FAITHFULNESS` | Adherence to provided context without fabrication |
| `COHERENCE` | Logical consistency and readability of responses |
| `SAFETY` | Avoidance of toxic, biased, or harmful content |
| `TOOL_USE` | Accuracy and appropriateness of tool selection and invocation |
| `TASK_COMPLETION` | Ability to fully accomplish assigned tasks |
| `CONTEXT_UTILIZATION` | Effective use of retrieval context and provided information |

Each dimension exposes `displayName()` and `description()` for human-readable output.

## DimensionBenchmark

A `DimensionBenchmark` record associates a dimension with its evaluation metrics and test cases:

```java
var benchmark = new DimensionBenchmark(
    CapabilityDimension.TOOL_USE,
    List.of(new ToolSelectionAccuracy(judge, 0.8)),
    toolUseTestCases
);
```

| Field | Type | Description |
|---|---|---|
| `dimension()` | `CapabilityDimension` | The dimension being benchmarked |
| `metrics()` | `List<EvalMetric>` | The metrics used to evaluate this dimension |
| `testCases()` | `List<AgentTestCase>` | The test cases for this dimension |

The profiler evaluates each benchmark by passing its test cases and metrics to `AgentEval.evaluate()` and averaging the resulting scores into a `ProfileScore`.

## Comparing Profiles

Use `CapabilityComparison.compare()` to diff two profiles. The result shows per-dimension deltas (B minus A), with positive values indicating improvement:

```java
CapabilityProfile v1 = profilerV1.profile();
CapabilityProfile v2 = profilerV2.profile();

CapabilityComparisonResult comparison =
    CapabilityComparison.compare(v1, v2);

// Overall score delta
double delta = comparison.overallDelta();

// Dimensions where v2 improved
List<CapabilityDimension> improved = comparison.improvements();

// Dimensions where v2 regressed
List<CapabilityDimension> regressed = comparison.regressions();

// Per-dimension deltas
Map<CapabilityDimension, Double> deltas = comparison.deltas();
```

## CapabilityReporter Output

`CapabilityReporter` prints formatted tables to the console.

### Print a Profile

```java
CapabilityReporter.printProfile(profile);
```

Sample output:

```
=== Capability Profile: my-agent-v2 ===
Overall Score: 0.812 | Duration: 4523ms

+----------------------+-------+----------------------------------------+
| Dimension            | Score | Reason                                 |
+----------------------+-------+----------------------------------------+
| Accuracy             | 0.850 | Average across 10 test cases and 1 ... |
| Safety               | 0.920 | Average across 8 test cases and 1 m... |
| Tool Use             | 0.667 | Average across 5 test cases and 1 m... |
+----------------------+-------+----------------------------------------+
Strengths: Accuracy, Safety
Weaknesses: none
```

### Print a Comparison

```java
CapabilityReporter.printComparison(comparison);
```

Sample output:

```
=== Comparison: my-agent-v1 vs my-agent-v2 ===
Overall Delta: +0.045

+----------------------+-------+-------+--------+
| Dimension            |     A |     B |  Delta |
+----------------------+-------+-------+--------+
| Accuracy             | 0.800 | 0.850 | +0.050 |
| Safety               | 0.900 | 0.920 | +0.020 |
| Tool Use             | 0.600 | 0.667 | +0.067 |
+----------------------+-------+-------+--------+
Improvements: Accuracy, Safety, Tool Use
Regressions: none
```

Both methods also accept a `PrintStream` argument to direct output to a file or buffer:

```java
CapabilityReporter.printProfile(profile, System.err);
CapabilityReporter.printComparison(comparison, new PrintStream("report.txt"));
```
