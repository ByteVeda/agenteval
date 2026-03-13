---
sidebar_position: 1
---

# Metrics Overview

AgentEval provides 25+ built-in metrics organized into five categories. All metrics return an `EvalScore` (0.0–1.0) with a human-readable reason.

## Response Quality

Evaluate how well the agent's response answers the user's question.

| Metric | Default Threshold | Requires LLM | Available Since |
|---|---|---|---|
| [AnswerRelevancy](./response-quality/answer-relevancy) | 0.7 | Yes | P0 |
| [Faithfulness](./response-quality/faithfulness) | 0.7 | Yes | P0 |
| [Hallucination](./response-quality/hallucination) | 0.5 | Yes | P0 |
| [Toxicity](./response-quality/toxicity) | 0.5 | Yes | P0 |
| [Correctness (G-Eval)](./response-quality/correctness) | 0.5 | Yes | P1 |
| [SemanticSimilarity](./response-quality/semantic-similarity) | 0.7 | No (embeddings) | P1 |
| [BiasDetection](./response-quality/bias-detection) | 0.5 | Yes | P1 |
| [Conciseness](./response-quality/conciseness) | 0.5 | Yes | P1 |
| [Coherence](./response-quality/coherence) | 0.7 | Yes | P1 |

## RAG Metrics

Evaluate the quality of Retrieval-Augmented Generation pipelines.

| Metric | Default Threshold | Requires LLM | Available Since |
|---|---|---|---|
| [ContextualPrecision](./rag/contextual-precision) | 0.7 | Yes | P1 |
| [ContextualRecall](./rag/contextual-recall) | 0.7 | Yes | P1 |
| [ContextualRelevancy](./rag/contextual-relevancy) | 0.7 | Yes | P1 |
| [RetrievalCompleteness](./rag/retrieval-completeness) | 0.8 | No | P1 |

## Agent Metrics

Evaluate tool use, planning, and execution trajectories.

| Metric | Default Threshold | Requires LLM | Available Since |
|---|---|---|---|
| [ToolSelectionAccuracy](./agent/tool-selection-accuracy) | 0.8 | No | P0 |
| [TaskCompletion](./agent/task-completion) | 0.5 | Yes | P0 |
| [ToolArgumentCorrectness](./agent/tool-argument-correctness) | 0.8 | No | P1 |
| [ToolResultUtilization](./agent/tool-result-utilization) | 0.7 | Yes | P1 |
| [PlanQuality](./agent/plan-quality) | 0.7 | Yes | P1 |
| [PlanAdherence](./agent/plan-adherence) | 0.7 | Yes | P1 |
| [TrajectoryOptimality](./agent/trajectory-optimality) | 0.5 | Yes | P1 |
| [StepLevelErrorLocalization](./agent/step-level-error-localization) | 0.5 | Yes | P1 |

## Conversation Metrics

Evaluate multi-turn conversation quality and coherence.

| Metric | Default Threshold | Requires LLM | Available Since |
|---|---|---|---|
| [ConversationCoherence](./conversation/conversation-coherence) | 0.7 | Yes | P1 |
| [ContextRetention](./conversation/context-retention) | 0.7 | Yes | P1 |
| [TopicDriftDetection](./conversation/topic-drift-detection) | 0.5 | Yes | P1 |
| [ConversationResolution](./conversation/conversation-resolution) | 0.5 | Yes | P1 |

## Custom Metrics

Build your own metrics using the G-Eval framework or pure Java logic.

| Type | When to Use |
|---|---|
| [G-Eval Custom Metric](./custom/g-eval) | Any evaluation criteria expressible in natural language |
| [Deterministic Metric](./custom/deterministic) | Rule-based checks — regex, JSON schema, keyword matching |
| [Composite Metric](./custom/composite) | Combine multiple metrics with weighted scoring |

## Using Multiple Metrics

```java
@Test
@AgentTest
@Metric(value = AnswerRelevancy.class, threshold = 0.7)
@Metric(value = Faithfulness.class, threshold = 0.8)
@Metric(value = ToolSelectionAccuracy.class, threshold = 0.9)
void comprehensiveEval() {
    var testCase = AgentTestCase.builder()
        .input("What is the status of order #12345?")
        .actualOutput(agent.run("What is the status of order #12345?"))
        .retrievalContext(retrievedDocs)
        .toolCalls(agent.getLastToolCalls())
        .expectedToolCalls(List.of(ToolCall.of("GetOrderStatus", Map.of("orderId", "12345"))))
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new AnswerRelevancy(0.7))
        .meetsMetric(new Faithfulness(0.8))
        .meetsMetric(new ToolSelectionAccuracy(0.9));
}
```

## Programmatic Batch Evaluation

```java
var metrics = List.of(
    new AnswerRelevancy(0.7),
    new Faithfulness(0.8),
    new ToolSelectionAccuracy(0.9)
);

EvalResults results = AgentEval.evaluate(dataset, metrics);
results.summary();       // prints console report
results.passRate();      // e.g. 0.94
results.averageScore();  // e.g. 0.87
```
