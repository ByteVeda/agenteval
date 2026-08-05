---
sidebar_position: 3
---

# ContextualRelevancy

Measures what proportion of retrieved documents are relevant to the user's query. Penalizes noisy retrieval that dilutes useful context.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `input`, `retrievalContext` |
| Available since | P1 |

## How It Works

Each retrieved document is evaluated for relevance to the input query. The score is the fraction of retrieved documents that are relevant: `relevant_docs / total_retrieved_docs`.

## Example

```java
var testCase = AgentTestCase.builder()
    .input("What is the refund policy?")
    .retrievalContext(List.of(
        "Full refund within 30 days of purchase.",          // relevant ✓
        "We offer free next-day shipping on all orders.",   // not relevant ✗
        "Returns must include original packaging.",          // relevant ✓
        "Our headquarters is in San Francisco."             // not relevant ✗
    ))
    .build();

EvalScore score = new ContextualRelevancy(0.7).evaluate(testCase);
// score.value()  → 0.5
// score.passed() → false
// score.reason() → "2 of 4 retrieved documents are relevant to the refund policy query.
//                   Shipping and headquarters documents should not have been retrieved."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ContextualRelevancy.class, threshold = 0.7)
void retrieverShouldNotReturnNoisyDocs() {
    var testCase = AgentTestCase.builder()
        .input(query)
        .retrievalContext(retrievedDocs)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new ContextualRelevancy(0.7));
}
```
