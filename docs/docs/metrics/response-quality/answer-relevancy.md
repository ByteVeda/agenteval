---
sidebar_position: 1
---

# AnswerRelevancy

Measures how relevant the agent's response is to the user's input question.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput` |
| Available since | P0 |

## How It Works

AnswerRelevancy uses an LLM to generate synthetic questions from the agent's output, then measures the semantic similarity between those generated questions and the original input. A response that directly addresses the question will produce similar re-generated questions; an irrelevant or off-topic response will not.

## Example

```java
var testCase = AgentTestCase.builder()
    .input("What is the capital of France?")
    .actualOutput("Paris is the capital of France. It is known for the Eiffel Tower.")
    .build();

EvalScore score = new AnswerRelevancy(0.7).evaluate(testCase);
// score.value()  → 0.95
// score.passed() → true
// score.reason() → "The response directly and accurately answers the question."
```

### Low relevancy example

```java
var testCase = AgentTestCase.builder()
    .input("What is the capital of France?")
    .actualOutput("France is a country in Western Europe with a rich cultural history.")
    .build();

EvalScore score = new AnswerRelevancy(0.7).evaluate(testCase);
// score.value()  → 0.31
// score.passed() → false
// score.reason() → "The response describes France but does not answer the question about its capital."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = AnswerRelevancy.class, threshold = 0.7)
void shouldBeRelevant() {
    var testCase = AgentTestCase.builder()
        .input(userQuery)
        .actualOutput(agent.run(userQuery))
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new AnswerRelevancy(0.7));
}
```

## Configuration

| Option | Type | Default | Description |
|---|---|---|---|
| `threshold` | `double` | `0.7` | Minimum score to pass |
| `strictMode` | `boolean` | `false` | Penalizes responses that answer a different question than asked |

```java
new AnswerRelevancy(0.8)                      // custom threshold
new AnswerRelevancy(0.8, true)                 // strict mode
```
