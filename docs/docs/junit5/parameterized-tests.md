---
sidebar_position: 4
---

# Parameterized Tests

Run evaluations across an entire dataset using JUnit 5's `@ParameterizedTest` with the `@DatasetSource` annotation.

## Basic Dataset Test

```java
@ExtendWith(AgentEvalExtension.class)
class GoldenSetEvalTest {

    private final RefundAgent agent = new RefundAgent();

    @ParameterizedTest
    @DatasetSource("src/test/resources/refund-golden-set.json")
    @Metric(value = AnswerRelevancy.class, threshold = 0.7)
    @Metric(value = Faithfulness.class, threshold = 0.8)
    void evaluateGoldenSet(AgentTestCase testCase) {
        testCase.setActualOutput(agent.run(testCase.getInput()));
    }
}
```

## Dataset Format

`refund-golden-set.json`:

```json
[
  {
    "input": "What is the refund window?",
    "expectedOutput": "30 days from purchase date.",
    "retrievalContext": [
      "Customers may return items within 30 days of purchase."
    ],
    "metadata": {
      "category": "policy",
      "difficulty": "easy"
    }
  },
  {
    "input": "Can I get a refund after 45 days?",
    "expectedOutput": "No, the return window is 30 days.",
    "retrievalContext": [
      "Returns are only accepted within 30 days of purchase."
    ],
    "metadata": {
      "category": "policy",
      "difficulty": "medium"
    }
  }
]
```

## CSV Dataset

```java
@ParameterizedTest
@DatasetSource("src/test/resources/test-cases.csv")
@Metric(value = AnswerRelevancy.class, threshold = 0.7)
void evaluateCsvDataset(AgentTestCase testCase) {
    testCase.setActualOutput(agent.run(testCase.getInput()));
}
```

`test-cases.csv`:

```csv
input,expectedOutput
"What is the refund window?","30 days from purchase."
"Can I return a digital product?","Digital products are non-refundable."
```

## JSONL Dataset (Streaming)

```java
@ParameterizedTest
@DatasetSource("src/test/resources/large-dataset.jsonl")
void evaluateLargeDataset(AgentTestCase testCase) {
    testCase.setActualOutput(agent.run(testCase.getInput()));
}
```

## Filter by Metadata

```java
@ParameterizedTest
@DatasetSource(value = "golden-set.json", filter = "metadata.category == 'refund'")
void evaluateRefundCategory(AgentTestCase testCase) { ... }
```

## Programmatic Dataset Loading

For more control, use the programmatic API:

```java
@Test
void programmaticBatchEval() throws Exception {
    var dataset = EvalDataset.load("src/test/resources/golden-set.json");

    // Run agent over entire dataset
    for (var testCase : dataset.cases()) {
        testCase.setActualOutput(agent.run(testCase.getInput()));
    }

    var results = AgentEval.evaluate(dataset,
        List.of(new AnswerRelevancy(0.7), new Faithfulness(0.8))
    );

    results.summary();                              // prints to console
    assertTrue(results.passRate() >= 0.90,
        "Pass rate below 90%: " + results.passRate());
}
```

## Aggregate Results

Fail the test if the overall pass rate drops below a threshold:

```java
@Test
void overallPassRateShouldExceed90Percent() {
    var results = AgentEval.evaluate(goldenSet,
        List.of(new AnswerRelevancy(0.7), new Faithfulness(0.8))
    );

    assertAll(
        () -> assertTrue(results.passRate() >= 0.90,
            "Pass rate: " + results.passRate()),
        () -> assertTrue(results.averageScore() >= 0.80,
            "Avg score: " + results.averageScore())
    );
}
```
