---
sidebar_position: 3
---

# ToolArgumentCorrectness

Measures whether the arguments passed to tools were correct. Goes beyond [ToolSelectionAccuracy](./tool-selection-accuracy) by validating what the agent passed to each tool, not just which tools were called.

| Property | Value |
|---|---|
| Default threshold | 0.8 |
| Requires LLM judge | No |
| Required fields | `toolCalls`, `expectedToolCalls` |
| Available since | P1 |

## Example

```java
var testCase = AgentTestCase.builder()
    .toolCalls(List.of(
        ToolCall.of("GetOrder", Map.of("orderId", "99999"))   // wrong ID
    ))
    .expectedToolCalls(List.of(
        ToolCall.of("GetOrder", Map.of("orderId", "12345"))   // should be 12345
    ))
    .build();

EvalScore score = new ToolArgumentCorrectness(0.8).evaluate(testCase);
// score.value()  → 0.0
// score.passed() → false
// score.reason() → "GetOrder called with orderId='99999', expected '12345'."
```

### Correct arguments

```java
var testCase = AgentTestCase.builder()
    .toolCalls(List.of(
        ToolCall.of("SearchOrders", Map.of(
            "customerId", "cust-42",
            "status", "pending"
        ))
    ))
    .expectedToolCalls(List.of(
        ToolCall.of("SearchOrders", Map.of(
            "customerId", "cust-42",
            "status", "pending"
        ))
    ))
    .build();

EvalScore score = new ToolArgumentCorrectness(0.8).evaluate(testCase);
// score.value() → 1.0
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ToolArgumentCorrectness.class, threshold = 0.9)
void agentShouldPassCorrectArguments() {
    var testCase = AgentTestCase.builder()
        .input("Get the status of order 12345 for customer C42.")
        .toolCalls(agent.getLastToolCalls())
        .expectedToolCalls(List.of(
            ToolCall.of("GetOrder", Map.of("orderId", "12345", "customerId", "C42"))
        ))
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new ToolArgumentCorrectness(0.9));
}
```

## Configuration

| Option | Type | Default | Description |
|---|---|---|---|
| `threshold` | `double` | `0.8` | Minimum score to pass |
| `strictMode` | `boolean` | `false` | Fail if agent passes extra arguments not in expected |
