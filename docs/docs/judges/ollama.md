---
sidebar_position: 5
---

# Ollama Judge (Local)

Run evaluations entirely locally with no API key required. Ideal for private data or offline environments.

## Setup

Install and start Ollama:

```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Pull a model
ollama pull llama3.2
ollama pull mistral

# Ollama runs at http://localhost:11434 by default
```

## Configuration

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.ollama("llama3.2"))
    .build();
```

Custom endpoint:

```java
JudgeModels.ollama("llama3.2", "http://my-ollama-server:11434")
```

## Recommended Models

| Model | Parameters | Quality Notes |
|---|---|---|
| `llama3.2` | 3B/8B | Good general-purpose judge |
| `mistral` | 7B | Strong instruction following |
| `llama3.1:70b` | 70B | Best quality for local evals |
| `qwen2.5:14b` | 14B | Good reasoning |

## YAML Configuration

```yaml
agenteval:
  judge:
    provider: ollama
    model: llama3.2
    endpoint: http://localhost:11434  # optional
```

:::tip
Ollama is the best choice when:
- Your test data contains sensitive/private information
- You want zero-cost evaluations (just compute)
- You need deterministic, reproducible results
:::
