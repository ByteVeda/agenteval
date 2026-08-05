---
sidebar_position: 2
---

# Faithfulness

Measures whether all claims in the agent's response are supported by the provided retrieval context. This is the core anti-hallucination metric for RAG systems.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput`, `retrievalContext` |
| Available since | P0 |

## How It Works

1. An LLM extracts individual factual claims from the agent's response
2. Each claim is verified against the retrieval context
3. The score is the proportion of claims that are supported: `supported_claims / total_claims`

A score of `1.0` means every claim in the response is grounded in the provided context. A lower score indicates the agent introduced facts not present in the retrieved documents.

## Example

```java
var testCase = AgentTestCase.builder()
    .input("What is our return window?")
    .actualOutput("You can return items within 30 days. We offer free return shipping.")
    .retrievalContext(List.of(
        "Customers may return items within 30 days of purchase.",
        "Return shipping is free for defective items only."
    ))
    .build();

EvalScore score = new Faithfulness(0.7).evaluate(testCase);
// score.value()  → 0.50
// score.passed() → false
// score.reason() → "Claim 'return shipping is free' is not fully supported — context
//                   states free shipping applies only to defective items."
```

### Fully faithful response

```java
var testCase = AgentTestCase.builder()
    .input("What is our return window?")
    .actualOutput("You can return items within 30 days of purchase.")
    .retrievalContext(List.of(
        "Customers may return items within 30 days of purchase."
    ))
    .build();

EvalScore score = new Faithfulness(0.7).evaluate(testCase);
// score.value()  → 1.0
// score.passed() → true
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = Faithfulness.class, threshold = 0.8)
void responseShouldBeFaithfulToContext() {
    var testCase = AgentTestCase.builder()
        .input(query)
        .actualOutput(agent.run(query))
        .retrievalContext(retrievedDocs)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new Faithfulness(0.8));
}
```

## Pair with AnswerRelevancy

Faithfulness and AnswerRelevancy together provide a complete picture of RAG quality:

- **AnswerRelevancy**: is the response on-topic?
- **Faithfulness**: is the response grounded in retrieved documents?

```java
AgentAssertions.assertThat(testCase)
    .meetsMetric(new AnswerRelevancy(0.7))
    .meetsMetric(new Faithfulness(0.8));
```
