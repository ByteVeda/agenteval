---
sidebar_position: 1
---

# ToolSelectionAccuracy

Measures whether the agent selected the correct tools to accomplish the task. Compares actual tool calls against expected tool calls by name.

| Property | Value |
|---|---|
| Default threshold | 0.8 |
| Requires LLM judge | No |
| Required fields | `toolCalls`, `expectedToolCalls` |
| Available since | P0 |

## How It Works

The metric computes the intersection over union between the set of actual tool names called and the set of expected tool names:

`score = |actual ∩ expected| / |actual ∪ expected|`

Order does not matter by default (configurable).

## Example

```java
var testCase = AgentTestCase.builder()
    .input("Cancel order #12345 and issue a refund.")
    .toolCalls(List.of(
        ToolCall.of("GetOrder", Map.of("orderId", "12345")),
        ToolCall.of("CancelOrder", Map.of("orderId", "12345")),
        ToolCall.of("IssueRefund", Map.of("orderId", "12345"))
    ))
    .expectedToolCalls(List.of(
        ToolCall.of("GetOrder", Map.of("orderId", "12345")),
        ToolCall.of("CancelOrder", Map.of("orderId", "12345")),
        ToolCall.of("IssueRefund", Map.of("orderId", "12345"))
    ))
    .build();

EvalScore score = new ToolSelectionAccuracy(0.8).evaluate(testCase);
// score.value()  → 1.0
// score.passed() → true
```

### Missing tool

```java
var testCase = AgentTestCase.builder()
    .toolCalls(List.of(
        ToolCall.of("GetOrder", Map.of("orderId", "12345")),
        ToolCall.of("CancelOrder", Map.of("orderId", "12345"))
        // missing: IssueRefund
    ))
    .expectedToolCalls(List.of(
        ToolCall.of("GetOrder", ...),
        ToolCall.of("CancelOrder", ...),
        ToolCall.of("IssueRefund", ...)
    ))
    .build();

EvalScore score = new ToolSelectionAccuracy(0.8).evaluate(testCase);
// score.value()  → 0.67
// score.passed() → false
// score.reason() → "Missing expected tool: IssueRefund"
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = ToolSelectionAccuracy.class, threshold = 0.9)
void agentShouldSelectCorrectTools() {
    var testCase = AgentTestCase.builder()
        .input("Cancel order #12345 and refund it.")
        .actualOutput(agent.run("Cancel order #12345 and refund it."))
        .toolCalls(agent.getLastToolCalls())
        .expectedToolCalls(List.of(
            ToolCall.of("GetOrder", Map.of("orderId", "12345")),
            ToolCall.of("CancelOrder", Map.of("orderId", "12345")),
            ToolCall.of("IssueRefund", Map.of("orderId", "12345"))
        ))
        .build();

    AgentAssertions.assertThat(testCase)
        .calledTool("GetOrder")
        .calledTool("CancelOrder")
        .calledTool("IssueRefund")
        .neverCalledTool("DeleteOrder")
        .meetsMetric(new ToolSelectionAccuracy(0.9));
}
```

## Configuration

| Option | Type | Default | Description |
|---|---|---|---|
| `threshold` | `double` | `0.8` | Minimum score to pass |
| `orderMatters` | `boolean` | `false` | Require tools called in the expected order |

```java
new ToolSelectionAccuracy(0.9, true)   // order must match
```
