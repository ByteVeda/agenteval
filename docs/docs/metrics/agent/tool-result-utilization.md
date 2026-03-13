---
sidebar_position: 4
---

# ToolResultUtilization

Measures whether the agent actually used the results from tool calls in its final response. Catches agents that call tools but ignore the results.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | Yes |
| Required fields | `actualOutput`, `toolCalls` (with results) |
| Available since | P1 |

## Example

```java
var testCase = AgentTestCase.builder()
    .input("What is the status of order #12345?")
    .actualOutput("I checked your order and it is currently being processed.")
    .toolCalls(List.of(
        ToolCall.builder()
            .name("GetOrder")
            .arguments(Map.of("orderId", "12345"))
            .result("{\"status\": \"DELIVERED\", \"deliveredAt\": \"2026-03-10\"}")
            .build()
    ))
    .build();

EvalScore score = new ToolResultUtilization(0.7).evaluate(testCase);
// score.value()  → 0.1
// score.passed() → false
// score.reason() → "Tool result shows order was DELIVERED on 2026-03-10, but response
//                   says 'being processed'. Agent ignored the actual tool result."
```

### Good utilization

```java
var testCase = AgentTestCase.builder()
    .input("What is the status of order #12345?")
    .actualOutput("Order #12345 has been delivered as of March 10th, 2026.")
    .toolCalls(List.of(
        ToolCall.builder()
            .name("GetOrder")
            .result("{\"status\": \"DELIVERED\", \"deliveredAt\": \"2026-03-10\"}")
            .build()
    ))
    .build();

EvalScore score = new ToolResultUtilization(0.7).evaluate(testCase);
// score.value() → 0.96
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ToolResultUtilization.class, threshold = 0.8)
void agentShouldUseToolResults() {
    var testCase = AgentTestCase.builder()
        .input(query)
        .actualOutput(agent.run(query))
        .toolCalls(agent.getLastToolCalls())   // must include tool results
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new ToolResultUtilization(0.8));
}
```
