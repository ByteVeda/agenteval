---
sidebar_position: 3
---

# Composite Metrics

Combine multiple metrics into a single weighted score. Useful for defining an overall quality gate that aggregates several dimensions.

## Basic Usage

```java
var overallQuality = CompositeMetric.builder()
    .name("OverallQuality")
    .add(new AnswerRelevancy(), 0.4)     // 40% weight
    .add(new Faithfulness(), 0.4)        // 40% weight
    .add(new Conciseness(), 0.2)         // 20% weight
    .strategy(CompositeStrategy.WEIGHTED_AVERAGE)
    .threshold(0.75)
    .build();

EvalScore score = overallQuality.evaluate(testCase);
// score.value()  → 0.82
// score.passed() → true
// score.reason() → "AnswerRelevancy: 0.87, Faithfulness: 0.91, Conciseness: 0.62
//                   Weighted average: 0.82"
```

## Strategies

### WEIGHTED_AVERAGE

All metrics contribute proportionally to their weight:

```java
.strategy(CompositeStrategy.WEIGHTED_AVERAGE)
```

### ALL_MUST_PASS

Every metric must individually meet its threshold:

```java
var strict = CompositeMetric.builder()
    .name("StrictQuality")
    .add(new AnswerRelevancy(0.7), 1.0)
    .add(new Faithfulness(0.8), 1.0)
    .add(new Toxicity(0.5), 1.0)
    .strategy(CompositeStrategy.ALL_MUST_PASS)
    .threshold(1.0)
    .build();
```

### ANY_MUST_PASS

At least one metric must pass:

```java
.strategy(CompositeStrategy.ANY_MUST_PASS)
```

## In JUnit 5

```java
@Test
@AgentTest
void overallQualityShouldPass() {
    var testCase = AgentTestCase.builder()
        .input(query)
        .actualOutput(agent.run(query))
        .retrievalContext(docs)
        .build();

    var overallQuality = CompositeMetric.builder()
        .name("OverallQuality")
        .add(new AnswerRelevancy(), 0.4)
        .add(new Faithfulness(), 0.4)
        .add(new Conciseness(), 0.2)
        .strategy(CompositeStrategy.WEIGHTED_AVERAGE)
        .threshold(0.75)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(overallQuality);
}
```
