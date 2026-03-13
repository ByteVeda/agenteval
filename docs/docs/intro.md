---
slug: /
sidebar_position: 1
---

# Introduction

**AgentEval** is a JUnit 5-native, local-first, framework-agnostic Java library for evaluating AI agent behavior.

## What is AgentEval?

AI agents produce non-deterministic outputs that standard unit tests can't cover. AgentEval fills that gap with:

- **25+ built-in metrics** — response quality, RAG pipelines, agent tool use, multi-turn conversations
- **LLM-as-judge** — pluggable providers (OpenAI, Anthropic, Google, Ollama, Azure, Bedrock)
- **JUnit 5 integration** — `@AgentTest`, `@Metric`, `@DatasetSource` annotations
- **Dataset management** — load golden sets from JSON/CSV/JSONL, run batch evaluations
- **Framework integrations** — optional auto-capture for Spring AI, LangChain4j, LangGraph4j, MCP

## Why AgentEval?

| Challenge | AgentEval Solution |
|---|---|
| Non-deterministic outputs | LLM-as-judge scoring with calibrated G-Eval rubrics |
| RAG quality | Faithfulness, ContextualRecall, ContextualPrecision metrics |
| Agent tool use | ToolSelectionAccuracy, ToolArgumentCorrectness metrics |
| Multi-turn coherence | ConversationCoherence, ContextRetention metrics |
| CI/CD integration | Standard JUnit XML, GitHub Actions, Maven/Gradle plugins |

## Design Principles

1. **Library, not framework** — evaluates agents, does not build them
2. **JUnit 5-native** — lives in your existing test suite
3. **Local-first** — no cloud, no SaaS; data never leaves your machine
4. **Framework-agnostic** — integrations for Spring AI, LangChain4j, etc. are optional add-ons

## Quick Example

```java
@ExtendWith(AgentEvalExtension.class)
class RefundAgentTest {

    @Test
    @AgentTest
    @Metric(value = AnswerRelevancy.class, threshold = 0.7)
    @Metric(value = Faithfulness.class, threshold = 0.8)
    void shouldAnswerRefundQuestions() {
        var testCase = AgentTestCase.builder()
            .input("What is our refund policy?")
            .actualOutput(agent.run("What is our refund policy?"))
            .retrievalContext(List.of(
                "Customers may request a full refund within 30 days of purchase.",
                "Refunds are processed within 5–7 business days."
            ))
            .build();

        AgentAssertions.assertThat(testCase)
            .meetsMetric(new AnswerRelevancy(0.7))
            .meetsMetric(new Faithfulness(0.8));
    }
}
```

## Next Steps

- [Installation](./getting-started/installation) — add AgentEval to your project
- [Quickstart](./getting-started/quickstart) — first evaluation in 5 minutes
- [Metrics Overview](./metrics/overview) — all 25+ available metrics
