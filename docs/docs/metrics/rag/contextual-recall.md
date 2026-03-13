---
sidebar_position: 2
---

# ContextualRecall

Measures whether the retrieval pipeline fetched all the information needed to generate the expected output. A low score means your retriever missed important documents.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `expectedOutput`, `retrievalContext` |
| Available since | P1 |

## How It Works

The metric aligns sentences in the expected output to sentences in the retrieved context. It measures what fraction of the expected output is attributable to the retrieved documents.

`score = sentences_attributed_to_context / total_sentences_in_expected_output`

## Example

```java
var testCase = AgentTestCase.builder()
    .expectedOutput("Items can be returned within 30 days. Free return shipping is provided.")
    .retrievalContext(List.of(
        "Customers may return items within 30 days of purchase.",
        "We offer next-day delivery on all orders."  // irrelevant
    ))
    .build();

EvalScore score = new ContextualRecall(0.7).evaluate(testCase);
// score.value()  → 0.5
// score.passed() → false
// score.reason() → "Only 1 of 2 expected sentences ('30-day return window') is covered
//                   by retrieved context. 'Free return shipping' is not present."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ContextualRecall.class, threshold = 0.8)
void retrieverShouldFetchAllNeededDocs() {
    var testCase = AgentTestCase.builder()
        .expectedOutput(groundTruth)
        .retrievalContext(retrievedDocs)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new ContextualRecall(0.8));
}
```
