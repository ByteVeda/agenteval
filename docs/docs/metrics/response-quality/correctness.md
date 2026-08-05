---
sidebar_position: 5
---

# Correctness (G-Eval)

A general-purpose evaluation metric based on the [G-Eval framework](https://arxiv.org/abs/2303.16634). Takes natural language evaluation criteria and uses chain-of-thought reasoning to produce a calibrated score.

| Property | Value |
|---|---|
| Default threshold | 0.5 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput`, `expectedOutput` |
| Available since | P1 |

## How It Works

G-Eval asks the judge LLM to:
1. Follow explicit evaluation steps (chain-of-thought)
2. Score the response on a 1–5 scale
3. Normalize to 0.0–1.0

This produces more calibrated scores than simple pass/fail judgments.

## Example

```java
var testCase = AgentTestCase.builder()
    .input("Explain how TLS handshakes work in simple terms.")
    .actualOutput(agent.run("Explain how TLS handshakes work in simple terms."))
    .expectedOutput("TLS handshake involves: client hello, server hello, certificate exchange, key exchange, and session establishment.")
    .build();

EvalScore score = new Correctness(0.5).evaluate(testCase);
// score.value()  → 0.78
// score.passed() → true
// score.reason() → "Response covers the main steps accurately but omits the session
//                   key establishment detail."
```

## Custom Criteria

The real power of Correctness is defining domain-specific evaluation criteria:

```java
var technicalAccuracy = Correctness.builder()
    .criteria("Evaluate whether the response contains technically accurate Java code")
    .evaluationSteps(List.of(
        "Check if all code snippets are syntactically valid Java",
        "Verify that API methods exist and are used correctly",
        "Check for use of deprecated APIs",
        "Assess whether the code compiles without errors"
    ))
    .threshold(0.8)
    .build();

EvalScore score = technicalAccuracy.evaluate(testCase);
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = Correctness.class, threshold = 0.7)
void responseShouldBeCorrect() {
    var testCase = AgentTestCase.builder()
        .input(question)
        .actualOutput(agent.run(question))
        .expectedOutput(groundTruth)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new Correctness(0.7));
}
```

## Configuration

| Option | Type | Default | Description |
|---|---|---|---|
| `threshold` | `double` | `0.5` | Minimum score to pass |
| `criteria` | `String` | Default correctness rubric | Natural language evaluation criteria |
| `evaluationSteps` | `List<String>` | Auto-generated | Chain-of-thought steps for the judge |
