---
sidebar_position: 3
---

# Golden Sets

Golden sets are curated, versioned test case collections that serve as the ground truth for your agent evaluations.

## What Is a Golden Set?

A golden set is a dataset where:
- Inputs are representative of real-world usage
- Expected outputs have been manually reviewed and approved
- The dataset is versioned and associated with a specific release

## Creating a Golden Set

```java
var goldenSet = EvalDataset.builder()
    .name("refund-agent-golden-v1")
    .version("1.0.0")
    .description("Curated refund policy queries for customer support agent")
    .addCase(AgentTestCase.builder()
        .input("What is the return window for electronics?")
        .expectedOutput("Electronics can be returned within 30 days of purchase with original packaging.")
        .retrievalContext(List.of(
            "Electronics return window: 30 days with original packaging.",
            "Opened electronics are subject to a 15% restocking fee."
        ))
        .metadata(Map.of(
            "category", "electronics",
            "difficulty", "medium",
            "reviewer", "alice@example.com"
        ))
        .build())
    .build();

goldenSet.save("src/test/resources/golden/refund-agent-golden-v1.json");
```

## Version Control

Commit your golden sets to version control alongside your code:

```
src/test/resources/golden/
  refund-agent-golden-v1.json    # version 1 — baseline
  refund-agent-golden-v2.json    # version 2 — added edge cases
```

Associate golden sets with code versions:

```java
var goldenSet = EvalDataset.builder()
    .name("refund-agent-golden")
    .version("2.0.0")
    .gitCommit("abc1234")    // associate with a specific commit
    .build();
```

## Running Against a Golden Set

```java
@ParameterizedTest
@DatasetSource("src/test/resources/golden/refund-agent-golden-v2.json")
@Metric(value = AnswerRelevancy.class, threshold = 0.8)
@Metric(value = Faithfulness.class, threshold = 0.85)
@Tag("eval")
void goldenSetEvaluation(AgentTestCase testCase) {
    testCase.setActualOutput(agent.run(testCase.getInput()));
}
```

## Regression Guard

Use golden sets as regression tests — fail CI if pass rate drops below a threshold:

```java
@Test
@Tag("eval")
void goldenSetPassRateShouldNotRegress() {
    var goldenSet = EvalDataset.load("golden/refund-agent-golden-v2.json");

    for (var tc : goldenSet.cases()) {
        tc.setActualOutput(agent.run(tc.getInput()));
    }

    var results = AgentEval.evaluate(goldenSet,
        List.of(new AnswerRelevancy(0.7), new Faithfulness(0.8))
    );

    assertTrue(results.passRate() >= 0.95,
        "Golden set pass rate regressed to: " + results.passRate());
}
```
