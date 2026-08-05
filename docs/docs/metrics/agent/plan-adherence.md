---
sidebar_position: 6
---

# PlanAdherence

Evaluates whether the agent followed its own plan during execution. Detects deviations, skipped steps, and unplanned actions.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `reasoningTrace` (must include both PLAN and ACTION steps) |
| Available since | P1 |

## Example

```java
var testCase = AgentTestCase.builder()
    .input("Process refund for order #12345.")
    .reasoningTrace(List.of(
        ReasoningStep.of(StepType.PLAN,
            "1. Retrieve order\n2. Validate eligibility\n3. Issue refund\n4. Notify customer"),
        ReasoningStep.of(StepType.ACTION, "Calling GetOrder(orderId=12345)"),
        // Step 2 skipped — went straight to issuing refund
        ReasoningStep.of(StepType.ACTION, "Calling IssueRefund(orderId=12345)"),
        ReasoningStep.of(StepType.ACTION, "Calling SendEmail(template=refund_confirmation)")
    ))
    .build();

EvalScore score = new PlanAdherence(0.7).evaluate(testCase);
// score.value()  → 0.58
// score.passed() → false
// score.reason() → "Step 2 (validate eligibility) was skipped. Agent issued refund
//                   without checking if order was eligible."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = PlanAdherence.class, threshold = 0.8)
void agentShouldFollowItsOwnPlan() {
    var testCase = AgentTestCase.builder()
        .input(task)
        .reasoningTrace(agent.getLastReasoningTrace())
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new PlanAdherence(0.8));
}
```
