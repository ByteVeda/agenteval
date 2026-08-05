---
sidebar_position: 8
---

# StepLevelErrorLocalization

Identifies which specific step in the agent's execution chain caused a failure. Useful for diagnosing root causes when the final output is wrong.

| Property | Value |
|---|---|
| Default threshold | 0.5 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput`, `reasoningTrace` |
| Available since | P1 |

## How It Works

Each reasoning step and tool call is evaluated individually. The metric produces a diagnostic report pointing to the first step where the agent's reasoning or actions went wrong.

## Example

```java
var testCase = AgentTestCase.builder()
    .input("Cancel order #12345 and send a refund confirmation.")
    .actualOutput("Your order has been cancelled. A confirmation email will be sent.")
    .reasoningTrace(List.of(
        ReasoningStep.builder()
            .type(StepType.ACTION)
            .content("Calling GetOrder(orderId=12345)")
            .toolCall(ToolCall.builder()
                .name("GetOrder")
                .result("{\"status\": \"ALREADY_CANCELLED\", \"refundIssued\": true}")
                .build())
            .build(),
        ReasoningStep.builder()
            .type(StepType.ACTION)
            .content("Calling CancelOrder(orderId=12345)")  // should have been skipped
            .toolCall(ToolCall.of("CancelOrder", Map.of("orderId", "12345")))
            .build()
    ))
    .build();

EvalScore score = new StepLevelErrorLocalization(0.5).evaluate(testCase);
// score.value()  → 0.3
// score.passed() → false
// score.reason() → "Error at Step 2: Agent attempted to cancel an already-cancelled order.
//                   Step 1 showed order status as ALREADY_CANCELLED, which should have
//                   triggered a different path."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = StepLevelErrorLocalization.class, threshold = 0.5)
void shouldLocateExecutionErrors() {
    var testCase = AgentTestCase.builder()
        .input(task)
        .actualOutput(agent.run(task))
        .reasoningTrace(agent.getLastReasoningTrace())
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new StepLevelErrorLocalization(0.5));
}
```

:::tip
This metric is most useful for **debugging** failed evaluations, not as a pass/fail gate. Run it when other metrics fail to identify what went wrong.
:::
