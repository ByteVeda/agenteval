---
sidebar_position: 9
---

# Coherence

Measures the logical flow, consistency, and readability of the agent's response. Checks for internal contradictions and poor structure.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput` |
| Available since | P1 |

## Example

```java
var testCase = AgentTestCase.builder()
    .input("Tell me about the refund process.")
    .actualOutput("To get a refund, submit a request online. However, all sales are final "
                + "and we do not accept refunds. You can expect your refund in 5–7 days.")
    .build();

EvalScore score = new Coherence(0.7).evaluate(testCase);
// score.value()  → 0.15
// score.passed() → false
// score.reason() → "Response contains a direct contradiction: it says to submit a refund
//                   request and then immediately states all sales are final."
```

```java
var testCase = AgentTestCase.builder()
    .input("Tell me about the refund process.")
    .actualOutput("To request a refund, visit our returns portal and submit a request "
                + "within 30 days of purchase. Approved refunds are processed in 5–7 business days.")
    .build();

EvalScore score = new Coherence(0.7).evaluate(testCase);
// score.value()  → 0.94
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = Coherence.class, threshold = 0.7)
void responseShouldBeCoherent() {
    var testCase = AgentTestCase.builder()
        .input(query)
        .actualOutput(agent.run(query))
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new Coherence(0.7));
}
```
