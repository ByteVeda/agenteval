---
sidebar_position: 3
---

# Hallucination

Detects fabricated information in the agent's response — invented entities, false statistics, non-existent citations, or facts contradicted by the provided context.

| Property | Value |
|---|---|
| Default threshold | 0.5 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput` (context optional but recommended) |
| Available since | P0 |

## How It Works

An LLM evaluates the response for:
- Facts that contradict the provided context
- Entities, numbers, or citations that appear fabricated
- Statements that cannot be verified from the context

The score represents the **absence** of hallucination: `1.0` = no hallucination detected, `0.0` = heavily hallucinated.

## Example

```java
// With context (recommended)
var testCase = AgentTestCase.builder()
    .input("Who founded our company?")
    .actualOutput("The company was founded in 1998 by John Smith and Lisa Chen.")
    .retrievalContext(List.of(
        "The company was founded in 2003 by Sarah Johnson."
    ))
    .build();

EvalScore score = new Hallucination(0.5).evaluate(testCase);
// score.value()  → 0.1
// score.passed() → false
// score.reason() → "Response states founding year as 1998 (context says 2003) and
//                   names founders John Smith and Lisa Chen (context says Sarah Johnson)."
```

### Without context

```java
// Works without context — uses common knowledge
var testCase = AgentTestCase.builder()
    .input("What is the boiling point of water?")
    .actualOutput("Water boils at 150°C at standard atmospheric pressure.")
    .build();

EvalScore score = new Hallucination(0.5).evaluate(testCase);
// score.value()  → 0.05
// score.passed() → false
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = Hallucination.class, threshold = 0.5)
void shouldNotHallucinate() {
    var testCase = AgentTestCase.builder()
        .input(query)
        .actualOutput(agent.run(query))
        .retrievalContext(retrievedDocs)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new Hallucination(0.5));
}
```

## Configuration

| Option | Type | Default | Description |
|---|---|---|---|
| `threshold` | `double` | `0.5` | Minimum score to pass |
| `contextRequired` | `boolean` | `false` | Fail if no context is provided |

:::tip
Use Hallucination alongside [Faithfulness](./faithfulness) for comprehensive grounding checks. Faithfulness measures claim-level support; Hallucination focuses on detecting invented facts.
:::
