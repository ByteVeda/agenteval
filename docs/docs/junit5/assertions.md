---
sidebar_position: 3
---

# AgentAssertions

Fluent assertion API for evaluating agent test cases. Throws `AssertionError` on failure with a detailed message including the metric score and reason.

## Starting an Assertion Chain

```java
AgentAssertions.assertThat(testCase)
    // chain assertions...
```

For conversations:

```java
AgentAssertions.assertThat(conversation)
    // chain assertions...
```

## Metric Assertions

### meetsMetric

Assert that a test case passes a specific metric:

```java
AgentAssertions.assertThat(testCase)
    .meetsMetric(new AnswerRelevancy(0.7))
    .meetsMetric(new Faithfulness(0.8))
    .meetsMetric(new ToolSelectionAccuracy(0.9));
```

On failure:

```
AssertionError: Metric 'Faithfulness' failed.
  Score: 0.42 (threshold: 0.80)
  Reason: "Claim 'return shipping is free' is not supported by the retrieved context."
```

## Tool Assertions

### calledTool

Assert that a specific tool was called:

```java
AgentAssertions.assertThat(testCase)
    .calledTool("GetOrder")
    .calledTool("CancelOrder");
```

### neverCalledTool

Assert that a specific tool was never called:

```java
AgentAssertions.assertThat(testCase)
    .neverCalledTool("DeleteOrder")
    .neverCalledTool("PermanentlyDeleteCustomer");
```

### toolCallCount

Assert the exact number of tool calls made:

```java
AgentAssertions.assertThat(testCase)
    .toolCallCount(3);
```

### hasToolCalls

Assert that at least one tool call was made:

```java
AgentAssertions.assertThat(testCase)
    .hasToolCalls();
```

### hasNoToolCalls

Assert that no tools were called:

```java
AgentAssertions.assertThat(testCase)
    .hasNoToolCalls();
```

## Output Assertions

### outputContains

Assert that the output contains a specific string:

```java
AgentAssertions.assertThat(testCase)
    .outputContains("30 days")
    .outputContains("refund");
```

### outputNotContains

Assert that the output does not contain a string:

```java
AgentAssertions.assertThat(testCase)
    .outputNotContains("error")
    .outputNotContains("I don't know");
```

### outputMatchesSchema

Assert that the output (parsed as JSON) conforms to a class schema:

```java
AgentAssertions.assertThat(testCase)
    .outputMatchesSchema(RefundResponse.class);
```

## Full Example

```java
@Test
@AgentTest
void comprehensiveRefundAgentTest() {
    var testCase = AgentTestCase.builder()
        .input("Cancel order #12345 and issue a refund.")
        .actualOutput(agent.run("Cancel order #12345 and issue a refund."))
        .retrievalContext(retrievedDocs)
        .toolCalls(agent.getLastToolCalls())
        .expectedToolCalls(expectedTools)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new AnswerRelevancy(0.7))
        .meetsMetric(new Faithfulness(0.8))
        .meetsMetric(new ToolSelectionAccuracy(0.9))
        .calledTool("GetOrder")
        .calledTool("CancelOrder")
        .calledTool("IssueRefund")
        .neverCalledTool("DeleteOrder")
        .outputContains("cancelled")
        .outputContains("refund");
}
```
