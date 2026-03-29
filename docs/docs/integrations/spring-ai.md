---
sidebar_position: 1
---

# Spring AI Integration

Auto-capture agent responses, tool calls, token usage, and RAG context from Spring AI applications.

## Dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-spring-ai</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

## Setup

Add the AgentEval auto-configuration bean in your test context:

```java
@SpringBootTest
@ExtendWith(AgentEvalExtension.class)
class RefundAgentSpringAiTest {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private AgentEvalCapture capture;  // auto-configured by agenteval-spring-ai

    @Test
    @AgentTest
    @Metric(value = AnswerRelevancy.class, threshold = 0.7)
    @Metric(value = Faithfulness.class, threshold = 0.8)
    void shouldAnswerRefundQuestions() {
        var response = chatClient.prompt()
            .user("What is the refund policy?")
            .call()
            .content();

        var testCase = capture.buildTestCase()
            .actualOutput(response)
            .build();

        AgentAssertions.assertThat(testCase)
            .meetsMetric(new AnswerRelevancy(0.7))
            .meetsMetric(new Faithfulness(0.8));
    }
}
```

## Auto-Captured Fields

The `agenteval-spring-ai` module automatically captures:

| Field | Source |
|---|---|
| `actualOutput` | `ChatResponse.getResult().getOutput().getContent()` |
| `tokenUsage` | `ChatResponse.getMetadata().getUsage()` |
| `toolCalls` | `ToolCallAdvisor` interception |
| `retrievalContext` | `QuestionAnswerAdvisor` / `VectorStoreAdvisor` context |

## Manual Test Case Construction

For full control, build the `AgentTestCase` manually:

```java
@Test
@AgentTest
@Metric(value = ToolSelectionAccuracy.class, threshold = 0.9)
void toolCallTest() {
    var toolCallCapture = new ToolCallCapture();
    var response = chatClient.prompt()
        .user("Cancel order #12345")
        .advisors(toolCallCapture)
        .call()
        .content();

    var testCase = AgentTestCase.builder()
        .input("Cancel order #12345")
        .actualOutput(response)
        .toolCalls(toolCallCapture.getCapturedToolCalls())
        .expectedToolCalls(List.of(
            ToolCall.of("GetOrder", Map.of("orderId", "12345")),
            ToolCall.of("CancelOrder", Map.of("orderId", "12345"))
        ))
        .build();

    AgentAssertions.assertThat(testCase)
        .calledTool("GetOrder")
        .calledTool("CancelOrder")
        .meetsMetric(new ToolSelectionAccuracy(0.9));
}
```
