---
sidebar_position: 1
---

# Test Case Model

`AgentTestCase` is the core data container that represents a single agent interaction to be evaluated.

## AgentTestCase

All fields use an immutable builder. Only `input` and `actualOutput` are required.

```java
var testCase = AgentTestCase.builder()
    .input("What is our refund policy?")
    .actualOutput(agent.run("What is our refund policy?"))
    .expectedOutput("Full refund within 30 days of purchase.")
    .retrievalContext(List.of(
        "Customers may request a full refund within 30 days.",
        "Refunds are processed within 5–7 business days."
    ))
    .context(List.of(
        "Full refund within 30 days of purchase."  // ground truth — what SHOULD have been retrieved
    ))
    .toolCalls(agent.getLastToolCalls())
    .expectedToolCalls(List.of(
        ToolCall.of("SearchKnowledgeBase", Map.of("query", "refund policy"))
    ))
    .latencyMs(342)
    .tokenUsage(TokenUsage.of(120, 85, 205))
    .metadata(Map.of("category", "refund", "difficulty", "easy"))
    .build();
```

### Field Reference

| Field | Type | Required | Description |
|---|---|---|---|
| `input` | `String` | Yes | User query or prompt sent to the agent |
| `actualOutput` | `String` | Yes | The agent's actual response |
| `expectedOutput` | `String` | No | Ground truth / ideal response |
| `retrievalContext` | `List<String>` | No | Documents retrieved by the RAG pipeline |
| `context` | `List<String>` | No | Ground truth context (should have been retrieved) |
| `toolCalls` | `List<ToolCall>` | No | Tools actually invoked during execution |
| `expectedToolCalls` | `List<ToolCall>` | No | Expected tool invocations |
| `reasoningTrace` | `List<ReasoningStep>` | No | Chain-of-thought / planning steps |
| `latencyMs` | `long` | No | End-to-end execution time in milliseconds |
| `tokenUsage` | `TokenUsage` | No | Input/output/total token counts |
| `cost` | `BigDecimal` | No | Estimated cost of the interaction |
| `metadata` | `Map<String, Object>` | No | Arbitrary key-value pairs for filtering |

## ConversationTestCase

For multi-turn conversations, use `ConversationTestCase`:

```java
var conversation = ConversationTestCase.builder()
    .conversationId("session-42")
    .systemPrompt("You are a helpful customer support agent.")
    .turn(AgentTestCase.builder()
        .input("Hi, I want to return something.")
        .actualOutput(agent.chat("Hi, I want to return something."))
        .build())
    .turn(AgentTestCase.builder()
        .input("It's order #5678.")
        .actualOutput(agent.chat("It's order #5678."))
        .build())
    .build();
```

## Supporting Types

### ToolCall

```java
// Simple tool call (no result)
ToolCall.of("SearchOrders", Map.of("orderId", "12345"));

// With result
ToolCall toolCall = ToolCall.builder()
    .name("SearchOrders")
    .arguments(Map.of("orderId", "12345"))
    .result("{\"status\": \"delivered\", \"date\": \"2026-01-15\"}")
    .durationMs(143)
    .build();
```

### ReasoningStep

```java
ReasoningStep step = ReasoningStep.builder()
    .type(StepType.THOUGHT)
    .content("I need to look up the order status before proceeding.")
    .build();

ReasoningStep action = ReasoningStep.builder()
    .type(StepType.ACTION)
    .content("Calling SearchOrders tool")
    .toolCall(ToolCall.of("SearchOrders", Map.of("orderId", "12345")))
    .build();
```

`StepType` values: `PLAN`, `THOUGHT`, `OBSERVATION`, `ACTION`

### TokenUsage

```java
TokenUsage usage = TokenUsage.of(120, 85, 205);  // input, output, total

// Or:
TokenUsage usage = new TokenUsage(120, 85, 205);
```

## Which Fields to Populate

Different metrics require different fields:

| Metric Category | Required Fields |
|---|---|
| Response quality (AnswerRelevancy, Correctness) | `input`, `actualOutput` |
| RAG metrics (Faithfulness, ContextualPrecision) | `input`, `actualOutput`, `retrievalContext` |
| Tool metrics | `toolCalls`, `expectedToolCalls` |
| Plan metrics | `reasoningTrace` |
| Conversation metrics | Use `ConversationTestCase` |
