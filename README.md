<p align="center">
  <img src="docs/static/img/logo.png" alt="AgentEval" width="80" />
</p>

<p align="center">
  <strong>Java AI Agent Evaluation & Testing Library</strong> — JUnit 5-native, local-first, framework-agnostic evaluation for AI agents.
</p>

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21+-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Build](https://img.shields.io/badge/build-Maven%20%7C%20Gradle-green.svg)](#build)

---

## Overview

AgentEval is a library (not a framework) for evaluating the quality of Java-based AI agents. It integrates directly into your existing JUnit 5 test suite and supports any AI framework — Spring AI, LangChain4j, LangGraph4j, MCP, or custom.

**Key principles:**
- **JUnit 5-native** — evaluations are standard test methods
- **Local-first** — no cloud, no SaaS, no data leaves the machine
- **Framework-agnostic** — optional integrations, zero forced dependencies
- **LLM-as-judge** — 7 pluggable judge providers with multi-model consensus

---

## Quick Start

### Maven

```xml
<dependency>
    <groupId>org.byteveda.agenteval</groupId>
    <artifactId>agenteval-junit5</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.byteveda.agenteval</groupId>
    <artifactId>agenteval-metrics</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

### Gradle

```kotlin
testImplementation("org.byteveda.agenteval:agenteval-junit5:0.1.0-SNAPSHOT")
testImplementation("org.byteveda.agenteval:agenteval-metrics:0.1.0-SNAPSHOT")
```

### Write Your First Evaluation

```java
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.junit5.annotation.AgentTest;
import org.byteveda.agenteval.junit5.annotation.Metric;
import org.byteveda.agenteval.metrics.response.AnswerRelevancyMetric;
import org.byteveda.agenteval.metrics.response.FaithfulnessMetric;

class MyAgentEvalTest {

    @AgentTest
    @Metric(value = AnswerRelevancyMetric.class, threshold = 0.7)
    @Metric(value = FaithfulnessMetric.class, threshold = 0.8)
    void testRefundPolicy() {
        var testCase = AgentTestCase.builder()
            .input("What is our refund policy?")
            .actualOutput(myAgent.ask("What is our refund policy?"))
            .retrievalContext(List.of(doc1, doc2))
            .build();

        AgentAssertions.assertThat(testCase).passesAllMetrics();
    }
}
```

Run only evaluation tests:

```bash
mvn test -Dgroups=eval
```

---

## Metrics

AgentEval ships 23 built-in metrics across 4 categories.

### Response Quality

| Metric | Description |
|--------|-------------|
| `AnswerRelevancyMetric` | Is the output relevant to the input question? |
| `FaithfulnessMetric` | Are claims in the output grounded in retrieval context? |
| `HallucinationMetric` | Does the output contain fabricated information? |
| `CorrectnessMetric` | G-Eval: flexible correctness against custom criteria |
| `SemanticSimilarityMetric` | Embedding-based cosine similarity to expected output |
| `CoherenceMetric` | Is the output logically coherent and well-structured? |
| `ConcisenessMetric` | Is the output appropriately concise? |
| `ToxicityMetric` | Does the output contain harmful content? |
| `BiasMetric` | Does the output exhibit gender, race, or other biases? |

### RAG Pipeline

| Metric | Description |
|--------|-------------|
| `ContextualRelevancyMetric` | Is the retrieved context relevant to the query? |
| `ContextualPrecisionMetric` | How precise is the retrieval (signal-to-noise ratio)? |
| `ContextualRecallMetric` | How much of the ground truth context was retrieved? |

### Agent Behavior

| Metric | Description |
|--------|-------------|
| `TaskCompletionMetric` | Did the agent complete the assigned task? |
| `ToolSelectionAccuracyMetric` | Did the agent call the correct tools? |
| `ToolArgumentCorrectnessMetric` | Were tool arguments correct? |
| `ToolResultUtilizationMetric` | Did the agent effectively use tool results? |
| `PlanQualityMetric` | Was the agent's plan coherent and executable? |
| `PlanAdherenceMetric` | Did the agent follow its stated plan? |
| `RetrievalCompletenessMetric` | Did the agent retrieve all necessary information? |
| `StepLevelErrorLocalizationMetric` | Can the first error step in the trajectory be identified? |
| `TrajectoryOptimalityMetric` | Was the agent's execution path efficient? |

### Conversation

| Metric | Description |
|--------|-------------|
| `ConversationCoherenceMetric` | Is the multi-turn conversation coherent? |
| `ContextRetentionMetric` | Does the agent retain context across turns? |
| `TopicDriftDetectionMetric` | Does the conversation stay on topic? |
| `ConversationResolutionMetric` | Was the user's goal ultimately resolved? |

All metrics implement `EvalMetric` and return `EvalScore` (value `0.0–1.0`, threshold, pass/fail, reason).

---

## Judge Providers

LLM-as-judge metrics require a configured judge provider.

| Provider | Class |
|----------|-------|
| OpenAI | `JudgeModels.openai()` |
| Anthropic | `JudgeModels.anthropic()` |
| Google Gemini | `JudgeModels.google()` |
| Azure OpenAI | `JudgeModels.azure()` |
| Amazon Bedrock | `JudgeModels.bedrock()` |
| Ollama (local) | `JudgeModels.ollama()` |
| Custom HTTP | `JudgeModels.custom()` (OpenAI-compatible: vLLM, LiteLLM, LocalAI) |

### Configuration

**Environment variables:**

```bash
AGENTEVAL_JUDGE_PROVIDER=openai
AGENTEVAL_JUDGE_MODEL=gpt-4o
OPENAI_API_KEY=sk-...
```

**Programmatic:**

```java
var config = AgentEvalConfig.builder()
    .judgeModel(JudgeModels.openai("gpt-4o", System.getenv("OPENAI_API_KEY")))
    .build();
```

**YAML (`agenteval.yaml`):**

```yaml
judge:
  provider: anthropic
  model: claude-3-5-sonnet-20241022
```

### Multi-Model Judge Consensus

```java
var judge = MultiModelJudge.builder()
    .addJudge(JudgeModels.openai(), 0.5)
    .addJudge(JudgeModels.anthropic(), 0.5)
    .strategy(ConsensusStrategy.WEIGHTED_AVERAGE)
    .build();
```

---

## Dataset-Driven Testing

Load test cases from JSON, CSV, or JSONL files:

```java
@AgentTest
@DatasetSource(path = "src/test/resources/qa-dataset.json")
@Metric(value = AnswerRelevancyMetric.class, threshold = 0.7)
void testDataset(AgentTestCase testCase) {
    testCase.setActualOutput(agent.ask(testCase.getInput()));
}
```

Generate synthetic datasets:

```java
var generator = new SyntheticDatasetGenerator(judgeModel);
var dataset = generator.fromDocuments(documents, 20);      // 20 cases from docs
var adversarial = generator.adversarial(baseDataset, 10);  // adversarial variants
```

---

## Reporting

AgentEval supports multiple report formats:

| Reporter | Output |
|----------|--------|
| `ConsoleReporter` | Colored terminal table |
| `JunitXmlReporter` | Standard JUnit XML (CI/CD compatible) |
| `JsonReporter` | Machine-readable JSON |
| `HtmlReporter` | Single-file self-contained HTML |

### Snapshot Testing

Lock in baseline scores and detect regressions:

```java
var store = new SnapshotStore(Path.of("src/test/snapshots"));
var reporter = new SnapshotReporter(store, SnapshotConfig.defaults());
reporter.report(result); // fails if score drops below baseline
```

### Regression Comparison

```java
var comparison = RegressionComparison.compare(baseline, current);
var report = RegressionReport.from(comparison);
```

---

## Benchmark Mode

Compare multiple agent variants side-by-side:

```java
var result = Benchmark.run(
    BenchmarkVariant.of("gpt-4o", testCase -> testCase.setActualOutput(gpt4oAgent.ask(testCase.getInput()))),
    BenchmarkVariant.of("claude-3-5", testCase -> testCase.setActualOutput(claudeAgent.ask(testCase.getInput()))),
    List.of(new AnswerRelevancyMetric(), new FaithfulnessMetric()),
    dataset
);
BenchmarkReporter.print(result);
```

---

## Framework Integrations

Optional modules for automatic capture with popular frameworks:

| Module | Artifact |
|--------|----------|
| Spring AI | `agenteval-spring-ai` |
| LangChain4j | `agenteval-langchain4j` |
| LangGraph4j | `agenteval-langgraph4j` |
| MCP Java SDK | `agenteval-mcp` |

---

## Build & CI/CD Plugins

### Maven Plugin

```xml
<plugin>
    <groupId>org.byteveda.agenteval</groupId>
    <artifactId>agenteval-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals><goal>evaluate</goal></goals>
        </execution>
    </executions>
</plugin>
```

```bash
mvn agenteval:evaluate
```

### Gradle Plugin

```kotlin
plugins {
    id("org.byteveda.agenteval.gradle-plugin") version "0.1.0-SNAPSHOT"
}

agenteval {
    reportFormat = "html"
    threshold = 0.7
}
```

### GitHub Actions

```yaml
- uses: agenteval/agenteval@v1
  with:
    report-format: markdown
    comment-on-pr: true
```

---

## Red Teaming

Adversarial evaluation with 20 built-in attack templates:

```java
var suite = RedTeamSuite.builder()
    .addAttacks(AttackTemplateLibrary.promptInjection())
    .addAttacks(AttackTemplateLibrary.jailbreak())
    .agent(myAgent)
    .evaluator(new AttackEvaluator(judgeModel))
    .build();

suite.run();
```

---

## Module Structure

```
agenteval-core/         — Test case model, metric interfaces, scoring engine, config
agenteval-metrics/      — 23 built-in metric implementations
agenteval-judge/        — LLM-as-judge engine, 7 provider integrations, multi-model consensus
agenteval-embeddings/   — Embedding model integrations (OpenAI, custom HTTP)
agenteval-junit5/       — JUnit 5 extension, @AgentTest, @Metric, @DatasetSource annotations
agenteval-datasets/     — JSON/CSV/JSONL loading, synthetic generation, golden set versioning
agenteval-reporting/    — Console, JUnit XML, JSON, HTML, snapshot, benchmark, regression reporters
agenteval-spring-ai/    — Spring AI auto-capture (optional)
agenteval-langchain4j/  — LangChain4j auto-capture (optional)
agenteval-langgraph4j/  — LangGraph4j graph execution capture (optional)
agenteval-mcp/          — MCP Java SDK tool call capture (optional)
agenteval-redteam/      — Adversarial testing, 20 attack templates
agenteval-maven-plugin/ — Maven build integration
agenteval-gradle-plugin/— Gradle build integration
agenteval-github-actions/ — GitHub Actions composite action
agenteval-intellij/     — IntelliJ IDEA tool window plugin
```

---

## Build

```bash
mvn clean install              # Build all modules
mvn test                       # Run all tests
mvn test -Dgroups=eval         # Run only evaluation tests
mvn test -DexcludeGroups=eval  # Skip evaluation tests (fast build)
mvn test -pl agenteval-core    # Test specific module
```

---

## Requirements

- Java 21+
- Maven 3.9+ or Gradle 8.5+

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
