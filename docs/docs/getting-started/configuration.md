---
sidebar_position: 3
---

# Configuration

AgentEval supports both programmatic configuration and file-based YAML configuration.

## Programmatic Configuration

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.openai("gpt-4o-mini"))
    .embeddingModel(EmbeddingModels.openai("text-embedding-3-small"))
    .defaultThreshold(0.7)
    .parallelism(4)
    .cacheResults(true)
    .costBudget(BigDecimal.valueOf(5.00))   // abort if eval run exceeds $5
    .build();

AgentEval.configure(config);
```

## YAML Configuration

Place `agenteval.yaml` in your project root or on the classpath:

```yaml
agenteval:
  judge:
    provider: openai          # openai | anthropic | google | ollama | azure | bedrock
    model: gpt-4o-mini
  embedding:
    provider: openai
    model: text-embedding-3-small
  defaults:
    threshold: 0.7
    parallelism: 4
  cache:
    enabled: true
    directory: .agenteval-cache
  cost:
    budget: 5.00              # max USD per eval run
```

## Environment Variables

All configuration values can be set via environment variables — no config file needed in CI:

| Variable | Purpose |
|---|---|
| `AGENTEVAL_JUDGE_PROVIDER` | Judge provider (`openai`, `anthropic`, `google`, `ollama`) |
| `AGENTEVAL_JUDGE_MODEL` | Judge model name |
| `OPENAI_API_KEY` | OpenAI API key |
| `ANTHROPIC_API_KEY` | Anthropic API key |
| `GOOGLE_API_KEY` | Google AI API key |
| `AZURE_OPENAI_API_KEY` | Azure OpenAI API key |
| `AZURE_OPENAI_ENDPOINT` | Azure OpenAI endpoint URL |

## Judge Providers

```java
// OpenAI
JudgeModels.openai("gpt-4o-mini")
JudgeModels.openai("gpt-4o")

// Anthropic
JudgeModels.anthropic("claude-haiku-4-5-20251001")
JudgeModels.anthropic("claude-sonnet-4-6")

// Google
JudgeModels.google("gemini-2.0-flash")
JudgeModels.google("gemini-2.5-pro")

// Ollama (local)
JudgeModels.ollama("llama3.2")
JudgeModels.ollama("mistral")

// Azure OpenAI
JudgeModels.azure("my-deployment-name", "https://my-resource.openai.azure.com/")

// Custom
JudgeModels.custom(new MyCustomJudgeProvider())
```

## Retry and Caching

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.openai("gpt-4o-mini"))
    .retryOnRateLimit(true, 3)       // retry up to 3 times on 429
    .cacheResults(true)              // cache judge responses to avoid redundant calls
    .cacheDirectory(".agenteval-cache")
    .build();
```

## Per-Test Judge Override

Override the judge model for a specific test class or method using `@JudgeModel`:

```java
@ExtendWith(AgentEvalExtension.class)
@JudgeModel(provider = "anthropic", model = "claude-sonnet-4-6")
class HighStakesEvalTest {

    @Test
    @AgentTest
    @JudgeModel(provider = "openai", model = "gpt-4o")  // method-level override
    @Metric(value = Faithfulness.class, threshold = 0.9)
    void criticalSafetyCheck() {
        // ...
    }
}
```

## Selective Test Execution

Use JUnit tags to control which evaluations run:

```bash
# Run only eval tests
mvn test -Dgroups=eval

# Skip eval tests (fast build)
mvn test -DexcludeGroups=eval

# Run only fast deterministic metrics
mvn test -Dgroups=eval-fast

# Run only a specific module's tests
mvn test -pl agenteval-core
```

Tag your tests:

```java
@Test
@Tag("eval")
@AgentTest
void myEval() { ... }
```
