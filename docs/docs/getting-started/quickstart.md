---
sidebar_position: 2
---

# Quickstart

This guide gets you from zero to your first evaluation in under 5 minutes.

## 1. Add the dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-junit5</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-judge</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

## 2. Set your API key

```bash
export OPENAI_API_KEY=sk-...
```

## 3. Write your first evaluation test

```java
import org.byteveda.agenteval.junit5.AgentEvalExtension;
import org.byteveda.agenteval.junit5.AgentTest;
import org.byteveda.agenteval.junit5.Metric;
import org.byteveda.agenteval.core.AgentTestCase;
import org.byteveda.agenteval.junit5.AgentAssertions;
import org.byteveda.agenteval.metrics.AnswerRelevancy;
import org.byteveda.agenteval.metrics.Faithfulness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(AgentEvalExtension.class)
class RefundAgentTest {

    // Your agent under test
    private final RefundAgent agent = new RefundAgent();

    @Test
    @AgentTest
    @Metric(value = AnswerRelevancy.class, threshold = 0.7)
    @Metric(value = Faithfulness.class, threshold = 0.8)
    void shouldAnswerRefundPolicy() {
        var testCase = AgentTestCase.builder()
            .input("What is the refund policy?")
            .actualOutput(agent.run("What is the refund policy?"))
            .retrievalContext(List.of(
                "Customers may request a full refund within 30 days of purchase.",
                "Refunds are processed to the original payment method within 5–7 business days."
            ))
            .build();

        AgentAssertions.assertThat(testCase)
            .meetsMetric(new AnswerRelevancy(0.7))
            .meetsMetric(new Faithfulness(0.8));
    }
}
```

## 4. Run the test

```bash
mvn test
# or
./gradlew test
```

You'll see evaluation scores in the test output:

```
[AgentEval] RefundAgentTest#shouldAnswerRefundPolicy
  AnswerRelevancy  0.87  PASS  (threshold: 0.70)
  Faithfulness     0.91  PASS  (threshold: 0.80)
```

## Evaluating agent tool calls

```java
@Test
@AgentTest
@Metric(value = ToolSelectionAccuracy.class, threshold = 0.9)
void shouldSelectCorrectTools() {
    var testCase = AgentTestCase.builder()
        .input("Cancel order #12345 and issue a refund")
        .actualOutput(agent.run("Cancel order #12345 and issue a refund"))
        .toolCalls(agent.getLastToolCalls())   // capture what tools were actually called
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

## Batch evaluation from a dataset

```java
@ParameterizedTest
@DatasetSource("src/test/resources/refund-golden-set.json")
@Metric(value = AnswerRelevancy.class, threshold = 0.7)
@Metric(value = Faithfulness.class, threshold = 0.8)
void evaluateGoldenSet(AgentTestCase testCase) {
    testCase.setActualOutput(agent.run(testCase.getInput()));
}
```

`refund-golden-set.json`:

```json
[
  {
    "input": "What is the refund window?",
    "expectedOutput": "30 days from purchase.",
    "retrievalContext": ["Full refund within 30 days of purchase."]
  },
  {
    "input": "How long do refunds take?",
    "expectedOutput": "5–7 business days.",
    "retrievalContext": ["Refunds processed within 5–7 business days."]
  }
]
```

## Next Steps

- [Configuration](./configuration) — configure the judge provider
- [Core Concepts](../core-concepts/test-case-model) — understand `AgentTestCase` fields
- [Metrics Overview](../metrics/overview) — all 25+ available metrics
- [JUnit 5 Annotations](../junit5/annotations) — full annotation reference
