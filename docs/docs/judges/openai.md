---
sidebar_position: 2
---

# OpenAI Judge

Use OpenAI models as the evaluation judge.

## Setup

```bash
export OPENAI_API_KEY=sk-...
```

## Configuration

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.openai("gpt-4o-mini"))
    .build();
```

## Recommended Models

| Model | Speed | Cost | Quality |
|---|---|---|---|
| `gpt-4o-mini` | Fast | Low | Good for most evals |
| `gpt-4o` | Medium | Medium | Better for complex criteria |
| `gpt-4.1` | Medium | Medium | Strong reasoning |
| `o3-mini` | Slow | Low | Best cost/quality ratio |

## Example

```java
// In test class
AgentEvalConfig.builder()
    .judgeModel(JudgeModels.openai("gpt-4o-mini"))
    .maxConcurrentJudgeCalls(4)
    .retryOnRateLimit(true, 3)
    .build();
```

## YAML Configuration

```yaml
agenteval:
  judge:
    provider: openai
    model: gpt-4o-mini
```
