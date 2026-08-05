---
sidebar_position: 3
---

# Anthropic Judge

Use Anthropic Claude models as the evaluation judge.

## Setup

```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

## Configuration

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.anthropic("claude-haiku-4-5-20251001"))
    .build();
```

## Recommended Models

| Model | Speed | Cost | Quality |
|---|---|---|---|
| `claude-haiku-4-5-20251001` | Fast | Low | Good for most evals |
| `claude-sonnet-4-6` | Medium | Medium | Excellent reasoning |
| `claude-opus-4-6` | Slow | High | Best quality |

## Example

```java
AgentEvalConfig.builder()
    .judgeModel(JudgeModels.anthropic("claude-sonnet-4-6"))
    .cacheResults(true)
    .build();
```

## YAML Configuration

```yaml
agenteval:
  judge:
    provider: anthropic
    model: claude-sonnet-4-6
```
