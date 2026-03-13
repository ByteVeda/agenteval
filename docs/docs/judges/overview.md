---
sidebar_position: 1
---

# Judges Overview

AgentEval uses LLM-as-judge for all non-deterministic metrics. The judge evaluates agent responses using research-backed prompt templates based on the G-Eval framework.

## Supported Providers

| Provider | Available Since | Notes |
|---|---|---|
| [OpenAI](./openai) | P0 | GPT-4o, GPT-4o-mini, GPT-4.1 |
| [Anthropic](./anthropic) | P0 | Claude Sonnet, Claude Haiku |
| [Google](./google) | P1 | Gemini 2.0 Flash, Gemini 2.5 Pro |
| [Ollama](./ollama) | P1 | Any locally hosted model — fully private |
| [Azure OpenAI](./custom) | P1 | Enterprise OpenAI endpoint |
| [Amazon Bedrock](./custom) | P1 | AWS-native model access |
| [Custom](./custom) | P0 | Implement `JudgeModel` interface |

## Quick Configuration

```java
// In AgentEvalConfig
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.openai("gpt-4o-mini"))
    .build();

AgentEval.configure(config);
```

Or via environment variable (no code change needed):

```bash
export AGENTEVAL_JUDGE_PROVIDER=anthropic
export AGENTEVAL_JUDGE_MODEL=claude-haiku-4-5-20251001
export ANTHROPIC_API_KEY=sk-ant-...
```

## Judge Features

### Prompt transparency

All judge prompt templates are stored as classpath resources and can be inspected or overridden:

```
agenteval-judge/src/main/resources/prompts/
  answer-relevancy.txt
  faithfulness.txt
  hallucination.txt
  ...
```

### Token tracking

Every LLM judge call tracks token usage:

```java
EvalResults results = AgentEval.evaluate(dataset, metrics);
results.judgeTokenUsage();    // total tokens used by judge
results.estimatedCost();      // estimated USD cost
```

### Result caching

Cache judge responses to avoid redundant LLM calls across runs:

```java
AgentEvalConfig.builder()
    .cacheResults(true)
    .cacheDirectory(".agenteval-cache")
    .build();
```

### Cost budget

Abort the evaluation run if costs exceed a threshold:

```java
AgentEvalConfig.builder()
    .costBudget(BigDecimal.valueOf(2.00))  // max $2 per run
    .build();
```

### Retry on rate limit

```java
AgentEvalConfig.builder()
    .retryOnRateLimit(true, 3)  // retry up to 3 times on 429
    .build();
```

## Parallel Judge Calls

AgentEval evaluates test cases concurrently using virtual threads:

```java
AgentEvalConfig.builder()
    .maxConcurrentJudgeCalls(8)   // default: available processors
    .build();
```
