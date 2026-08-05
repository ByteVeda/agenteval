---
sidebar_position: 1
---

# ContextualPrecision

Measures whether relevant retrieved documents are ranked higher than irrelevant ones. A high score means your retriever surfaces the most useful documents at the top of the list.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput`, `retrievalContext`, `expectedOutput` |
| Available since | P1 |

## How It Works

For each retrieved document, the LLM determines whether it is relevant to answering the question. The metric rewards systems where relevant documents appear earlier in the retrieval list (weighted precision).

## Example

```java
var testCase = AgentTestCase.builder()
    .input("What is our return policy?")
    .expectedOutput("Items can be returned within 30 days.")
    .actualOutput(agent.run("What is our return policy?"))
    .retrievalContext(List.of(
        "Customers may return items within 30 days of purchase.",  // relevant (rank 1) ✓
        "Our store hours are Monday through Friday, 9am–5pm.",      // irrelevant (rank 2) ✗
        "Returns must include original packaging."                  // relevant (rank 3) ✓
    ))
    .build();

EvalScore score = new ContextualPrecision(0.7).evaluate(testCase);
// score.value()  → 0.67
// score.reason() → "Relevant doc at rank 1 (good), irrelevant doc at rank 2 penalizes
//                   precision score. Relevant doc at rank 3 partially recovers."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ContextualPrecision.class, threshold = 0.7)
void retrieverShouldRankRelevantDocsFirst() {
    var testCase = AgentTestCase.builder()
        .input(query)
        .expectedOutput(groundTruth)
        .actualOutput(agent.run(query))
        .retrievalContext(retrievedDocs)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new ContextualPrecision(0.7));
}
```

:::tip
Use ContextualPrecision together with [ContextualRecall](./contextual-recall) and [ContextualRelevancy](./contextual-relevancy) to get a full picture of your RAG pipeline quality.
:::
