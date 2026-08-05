---
sidebar_position: 2
---

# Annotations

Full reference for all AgentEval JUnit 5 annotations.

## @AgentTest

Marks a test method as an agent evaluation. Must be used alongside `@ExtendWith(AgentEvalExtension.class)`.

```java
@Test
@AgentTest
void myEvaluation() { ... }
```

## @Metric

Applies a metric with a configurable threshold. Repeatable — stack multiple `@Metric` annotations on one test.

```java
@Test
@AgentTest
@Metric(value = AnswerRelevancy.class, threshold = 0.7)
@Metric(value = Faithfulness.class, threshold = 0.8)
@Metric(value = Toxicity.class, threshold = 0.5)
void comprehensiveEval() { ... }
```

| Attribute | Type | Description |
|---|---|---|
| `value` | `Class<? extends EvalMetric>` | The metric class to apply |
| `threshold` | `double` | Minimum score to pass (default: 0.7) |

## @Metrics

Container annotation for multiple `@Metric` annotations (used when targeting Java 8 — not required in Java 21):

```java
@Metrics({
    @Metric(value = AnswerRelevancy.class, threshold = 0.7),
    @Metric(value = Faithfulness.class, threshold = 0.8)
})
```

## @DatasetSource

Loads test cases from a file and runs the test as a parameterized test. Supports JSON, CSV, and JSONL.

```java
@ParameterizedTest
@DatasetSource("src/test/resources/golden-set.json")
@Metric(value = AnswerRelevancy.class, threshold = 0.7)
void evaluateDataset(AgentTestCase testCase) {
    testCase.setActualOutput(agent.run(testCase.getInput()));
}
```

| Attribute | Description |
|---|---|
| `value` | Path to the dataset file (classpath or filesystem) |

## @GoldenSet

Injects a golden dataset as a parameter in a parameterized test. Provides the full `EvalDataset` object rather than individual cases.

```java
@ParameterizedTest
void evaluateGoldenSet(@GoldenSet("refund-queries.json") EvalDataset dataset) {
    // use dataset.cases() to iterate
}
```

## @JudgeModel

Overrides the judge LLM for a specific test class or method.

```java
@ExtendWith(AgentEvalExtension.class)
@JudgeModel(provider = "anthropic", model = "claude-sonnet-4-6")
class MyTest {

    @Test
    @AgentTest
    @JudgeModel(provider = "openai", model = "gpt-4o")  // method-level overrides class
    void expensiveEval() { ... }
}
```

| Attribute | Description |
|---|---|
| `provider` | Judge provider (`openai`, `anthropic`, `google`, `ollama`) |
| `model` | Model identifier |

## @EvalTimeout

Sets a maximum duration for an evaluation. The test fails if the evaluation exceeds this time.

```java
@Test
@AgentTest
@EvalTimeout(seconds = 30)
void timeLimitedEval() { ... }
```

## @Tag("eval")

Standard JUnit 5 tag. Use to group evaluation tests for selective execution.

```java
@Test
@Tag("eval")
@AgentTest
void myEval() { ... }

// Run: mvn test -Dgroups=eval
// Skip: mvn test -DexcludeGroups=eval
```
