---
sidebar_position: 6
---

# SemanticSimilarity

Measures the semantic similarity between the agent's actual output and the expected output using embedding-based cosine similarity. No LLM judge calls required — fast and deterministic.

| Property | Value |
|---|---|
| Default threshold | 0.7 |
| Requires LLM judge | No (uses embeddings) |
| Required fields | `actualOutput`, `expectedOutput` |
| Available since | P1 |

## How It Works

Both the actual and expected outputs are converted to embedding vectors. The score is the cosine similarity between the two vectors:

- `1.0` — semantically identical
- `0.7–0.9` — semantically similar (different phrasing, same meaning)
- `< 0.5` — semantically different

## Example

```java
var testCase = AgentTestCase.builder()
    .actualOutput("You may return products within thirty days of buying them.")
    .expectedOutput("Items can be returned within 30 days of purchase.")
    .build();

EvalScore score = new SemanticSimilarity(0.7).evaluate(testCase);
// score.value()  → 0.93
// score.passed() → true
```

```java
var testCase = AgentTestCase.builder()
    .actualOutput("Contact us at support@example.com for help.")
    .expectedOutput("Items can be returned within 30 days of purchase.")
    .build();

EvalScore score = new SemanticSimilarity(0.7).evaluate(testCase);
// score.value()  → 0.21
// score.passed() → false
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = SemanticSimilarity.class, threshold = 0.8)
void responseShouldMatchExpected() {
    var testCase = AgentTestCase.builder()
        .actualOutput(agent.run(query))
        .expectedOutput(goldenAnswer)
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new SemanticSimilarity(0.8));
}
```

## Configure Embedding Model

```java
AgentEvalConfig config = AgentEvalConfig.builder()
    .embeddingModel(EmbeddingModels.openai("text-embedding-3-small"))
    .build();

// Or use a local embedding model
AgentEvalConfig config = AgentEvalConfig.builder()
    .embeddingModel(EmbeddingModels.ollama("nomic-embed-text"))
    .build();
```

:::tip
SemanticSimilarity is ideal for **regression testing** — fast, cheap, and deterministic. Use it alongside LLM-judge metrics for a complete picture.
:::
