---
sidebar_position: 4
---

# RetrievalCompleteness

Checks whether all required ground truth documents were retrieved. A set-based metric that does not require an LLM judge.

| Property | Value |
|---|---|
| Default threshold | 0.8 |
| Requires LLM judge | No |
| Required fields | `retrievalContext`, `context` (ground truth) |
| Available since | P1 |

## How It Works

The metric compares the set of retrieved documents (`retrievalContext`) against the set of ground truth documents that should have been retrieved (`context`).

`score = retrieved_ground_truth_docs / total_ground_truth_docs`

Supports exact match and semantic (embedding-based) matching.

## Example

```java
var testCase = AgentTestCase.builder()
    .context(List.of(                                   // ground truth: must be retrieved
        "Full refund within 30 days.",
        "Returns require original packaging.",
        "Refunds processed in 5–7 business days."
    ))
    .retrievalContext(List.of(                           // what was actually retrieved
        "Full refund within 30 days.",
        "Refunds processed in 5–7 business days.",
        "We offer next-day delivery."                   // not in ground truth
    ))
    .build();

EvalScore score = new RetrievalCompleteness(0.8).evaluate(testCase);
// score.value()  → 0.67
// score.passed() → false
// score.reason() → "2 of 3 required documents retrieved. Missing: 'Returns require original packaging.'"
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = RetrievalCompleteness.class, threshold = 0.9)
void retrieverShouldFetchAllGroundTruthDocs() {
    var testCase = AgentTestCase.builder()
        .context(requiredDocuments)
        .retrievalContext(retrievedDocs)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new RetrievalCompleteness(0.9));
}
```

## Match Modes

```java
new RetrievalCompleteness(0.8, MatchMode.EXACT)     // string equality
new RetrievalCompleteness(0.8, MatchMode.SEMANTIC)  // embedding cosine similarity
```
