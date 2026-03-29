---
sidebar_position: 2
---

# LangChain4j Integration

Auto-capture responses, tool calls, and RAG context from LangChain4j AI services.

## Dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-langchain4j</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

## Setup

Wrap your `AiService` with the AgentEval capture proxy:

```java
@ExtendWith(AgentEvalExtension.class)
class RefundAgentLc4jTest {

    interface RefundAssistant {
        String answer(String question);
    }

    private final AgentEvalCapture capture = new AgentEvalCapture();

    private final RefundAssistant assistant = AiServices.builder(RefundAssistant.class)
        .chatLanguageModel(openAiChatModel)
        .tools(new RefundTools())
        .contentRetriever(embeddingStoreRetriever)
        .interceptor(capture.interceptor())   // attach capture
        .build();

    @Test
    @AgentTest
    @Metric(value = AnswerRelevancy.class, threshold = 0.7)
    @Metric(value = Faithfulness.class, threshold = 0.8)
    void shouldAnswerRefundQuestions() {
        String response = assistant.answer("What is the refund policy?");

        var testCase = capture.buildTestCase()
            .input("What is the refund policy?")
            .actualOutput(response)
            .build();

        AgentAssertions.assertThat(testCase)
            .meetsMetric(new AnswerRelevancy(0.7))
            .meetsMetric(new Faithfulness(0.8));
    }
}
```

## Auto-Captured Fields

| Field | Source |
|---|---|
| `tokenUsage` | `TokenUsage` from `Response` |
| `toolCalls` | `AiMessage.toolExecutionRequests()` |
| `retrievalContext` | `ContentRetriever.retrieve()` results |

## Tool Call Evaluation

```java
@Test
@AgentTest
@Metric(value = ToolSelectionAccuracy.class, threshold = 0.9)
@Metric(value = ToolArgumentCorrectness.class, threshold = 0.9)
void toolSelectionTest() {
    String response = assistant.answer("Cancel order #12345 and issue refund.");

    var testCase = capture.buildTestCase()
        .input("Cancel order #12345 and issue refund.")
        .actualOutput(response)
        .expectedToolCalls(List.of(
            ToolCall.of("getOrder", Map.of("orderId", "12345")),
            ToolCall.of("cancelOrder", Map.of("orderId", "12345")),
            ToolCall.of("issueRefund", Map.of("orderId", "12345"))
        ))
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new ToolSelectionAccuracy(0.9))
        .meetsMetric(new ToolArgumentCorrectness(0.9));
}
```
