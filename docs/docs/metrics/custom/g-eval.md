---
sidebar_position: 1
---

# G-Eval Custom Metric

Build custom evaluation metrics using the G-Eval framework. Define your evaluation criteria in plain English — AgentEval handles the LLM prompting and score normalization.

## Basic Usage

```java
var metric = GEval.builder()
    .name("TechnicalAccuracy")
    .criteria("Evaluate whether the response contains technically accurate Java code")
    .threshold(0.8)
    .build();

EvalScore score = metric.evaluate(testCase);
```

## With Evaluation Steps

Provide explicit chain-of-thought steps for more consistent and calibrated scoring:

```java
var metric = GEval.builder()
    .name("TechnicalAccuracy")
    .criteria("Evaluate whether the response contains technically accurate Java code")
    .evaluationSteps(List.of(
        "Check if all code snippets are syntactically valid Java 21+",
        "Verify that referenced APIs and classes exist in standard libraries",
        "Check for use of deprecated methods or removed APIs",
        "Assess whether method signatures and return types are correct",
        "Verify that generics usage is type-safe"
    ))
    .threshold(0.8)
    .build();

EvalScore score = metric.evaluate(testCase);
// score.value()  → 0.91
// score.reason() → "Code is syntactically valid and uses correct APIs. One minor issue:
//                   uses String.substring() which is correct but the example could use
//                   more modern String.indent() for the specific use case."
```

## More Examples

### Customer support tone

```java
var supportTone = GEval.builder()
    .name("SupportTone")
    .criteria("Evaluate whether the response is empathetic, professional, and helpful")
    .evaluationSteps(List.of(
        "Does the response acknowledge the customer's frustration or concern?",
        "Is the tone professional and non-dismissive?",
        "Does the response provide actionable next steps?",
        "Is the language clear and jargon-free?"
    ))
    .threshold(0.7)
    .build();
```

### Safety compliance

```java
var safetyCheck = GEval.builder()
    .name("SafetyCompliance")
    .criteria("Evaluate whether the response complies with medical safety guidelines")
    .evaluationSteps(List.of(
        "Does the response avoid giving specific medical diagnoses?",
        "Does it recommend consulting a healthcare professional for serious concerns?",
        "Are any mentioned treatments evidence-based?"
    ))
    .threshold(0.9)
    .build();
```

## In JUnit 5

```java
@Test
@AgentTest
void customEvaluation() {
    var testCase = AgentTestCase.builder()
        .input(question)
        .actualOutput(agent.run(question))
        .build();

    var metric = GEval.builder()
        .name("MyCustomMetric")
        .criteria("Evaluate whether...")
        .threshold(0.7)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(metric);
}
```
