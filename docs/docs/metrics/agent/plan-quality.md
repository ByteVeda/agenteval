---
sidebar_position: 5
---

# PlanQuality

Evaluates whether the agent's generated plan is logical, complete, and efficient. Requires access to the agent's reasoning trace.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `input`, `reasoningTrace` |
| Available since | P1 |

## What It Checks

- Does the plan address all aspects of the user's request?
- Are the steps in a sensible order?
- Are there redundant or unnecessary steps?
- Is the plan achievable with the available tools?

## Example

```java
var testCase = AgentTestCase.builder()
    .input("Process a refund for order #12345.")
    .reasoningTrace(List.of(
        ReasoningStep.builder()
            .type(StepType.PLAN)
            .content("1. Retrieve order #12345 details\n"
                   + "2. Verify the order is eligible for refund\n"
                   + "3. Process the refund\n"
                   + "4. Send confirmation email to customer")
            .build()
    ))
    .build();

EvalScore score = new PlanQuality(0.7).evaluate(testCase);
// score.value()  → 0.90
// score.passed() → true
// score.reason() → "Plan is logical, covers all required steps in correct order."
```

### Poor plan

```java
.reasoningTrace(List.of(
    ReasoningStep.of(StepType.PLAN,
        "1. Send refund confirmation\n"
        + "2. Maybe check if order exists")  // backwards order, vague steps
))

// score.value() → 0.2
// score.reason() → "Plan sends confirmation before verifying order exists.
//                   Step 2 is vague and non-actionable."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = PlanQuality.class, threshold = 0.7)
void agentPlanShouldBeLogical() {
    var testCase = AgentTestCase.builder()
        .input(complexTask)
        .reasoningTrace(agent.getLastReasoningTrace())
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new PlanQuality(0.7));
}
```
