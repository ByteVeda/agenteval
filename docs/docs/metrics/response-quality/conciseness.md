---
sidebar_position: 8
---

# Conciseness

Measures whether the agent's response is appropriately concise — penalizes verbosity, repetition, and filler content without losing information.

| Property | Value |
|---|---|
| Default threshold | 0.5 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput` |
| Available since | P1 |

## Example

```java
var testCase = AgentTestCase.builder()
    .input("What is 2 + 2?")
    .actualOutput("Great question! I'm happy to help you with that mathematical problem. "
                + "When we consider the addition operation and apply it to the numbers 2 and 2, "
                + "we get the result of 4. So, to summarize my answer: 2 + 2 = 4.")
    .build();

EvalScore score = new Conciseness(0.5).evaluate(testCase);
// score.value()  → 0.2
// score.passed() → false
// score.reason() → "Response is excessively verbose for a simple arithmetic question.
//                   A concise answer would be: '2 + 2 = 4.'"
```

```java
var testCase = AgentTestCase.builder()
    .input("What is 2 + 2?")
    .actualOutput("2 + 2 = 4.")
    .build();

EvalScore score = new Conciseness(0.5).evaluate(testCase);
// score.value()  → 0.99
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = Conciseness.class, threshold = 0.6)
void responseShouldBeConcise() {
    var testCase = AgentTestCase.builder()
        .input(query)
        .actualOutput(agent.run(query))
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new Conciseness(0.6));
}
```
