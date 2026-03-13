---
sidebar_position: 2
---

# EvalScore

Every metric in AgentEval returns an `EvalScore` — a record that contains the numeric score, pass/fail verdict, and a human-readable reason.

## Structure

```java
public record EvalScore(
    String metricName,   // e.g. "AnswerRelevancy"
    double value,        // 0.0 – 1.0
    double threshold,    // configured threshold
    boolean passed,      // value >= threshold
    String reason        // LLM-generated or computed explanation
) {}
```

## Example Output

```java
EvalScore score = new AnswerRelevancy(0.7).evaluate(testCase);

System.out.println(score.value());      // 0.87
System.out.println(score.passed());     // true
System.out.println(score.reason());
// "The response directly addresses the user's question about refund policy
//  and provides the key details (30-day window, processing time)."
```

## Accessing Scores

### From assertions

```java
AgentAssertions.assertThat(testCase)
    .meetsMetric(new AnswerRelevancy(0.7))   // throws AssertionError if score < 0.7
    .meetsMetric(new Faithfulness(0.8));
```

### From programmatic evaluation

```java
List<EvalScore> scores = AgentEval.evaluate(testCase,
    List.of(new AnswerRelevancy(0.7), new Faithfulness(0.8))
);

for (EvalScore score : scores) {
    System.out.printf("%s: %.2f (%s)%n",
        score.metricName(), score.value(), score.passed() ? "PASS" : "FAIL");
}
```

### From batch evaluation

```java
EvalResults results = AgentEval.evaluate(dataset,
    List.of(new AnswerRelevancy(0.7), new Faithfulness(0.8))
);

System.out.println("Pass rate: " + results.passRate());        // 0.94
System.out.println("Avg score: " + results.averageScore());    // 0.86

results.failedCases().forEach(c ->
    System.out.println("FAILED: " + c.testCase().getInput())
);
```

## Score Normalization

All metric scores are normalized to the **0.0–1.0** range:

- `1.0` — perfect score
- `0.0` — complete failure
- `passed = (value >= threshold)`

The default threshold is `0.7` unless specified otherwise.

## Threshold Configuration

```java
// Via constructor
new AnswerRelevancy(0.8)

// Via annotation
@Metric(value = AnswerRelevancy.class, threshold = 0.8)

// Via global default
AgentEvalConfig.builder().defaultThreshold(0.7).build()
```
