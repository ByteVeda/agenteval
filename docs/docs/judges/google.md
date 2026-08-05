---
sidebar_position: 4
---

# Google Judge

Use Google Gemini models as the evaluation judge.

## Setup

```bash
export GOOGLE_API_KEY=...
```

## Configuration

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.google("gemini-2.0-flash"))
    .build();
```

## Recommended Models

| Model | Speed | Cost | Quality |
|---|---|---|---|
| `gemini-2.0-flash` | Fast | Low | Good for most evals |
| `gemini-2.5-pro` | Medium | Medium | Best reasoning |

## YAML Configuration

```yaml
agenteval:
  judge:
    provider: google
    model: gemini-2.0-flash
```
