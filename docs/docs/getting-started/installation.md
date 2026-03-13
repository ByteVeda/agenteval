---
sidebar_position: 1
---

# Installation

AgentEval is distributed via Maven Central. The minimum requirement is **Java 21**.

## Maven

Add the JUnit 5 integration module (the most common starting point):

```xml
<dependency>
  <groupId>com.agenteval</groupId>
  <artifactId>agenteval-junit5</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

For LLM-as-judge metrics (AnswerRelevancy, Faithfulness, etc.), also add the judge module:

```xml
<dependency>
  <groupId>com.agenteval</groupId>
  <artifactId>agenteval-judge</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

### Bill of Materials

Use the BOM to manage versions across multiple modules:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.agenteval</groupId>
      <artifactId>agenteval-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.agenteval</groupId>
    <artifactId>agenteval-junit5</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>com.agenteval</groupId>
    <artifactId>agenteval-judge</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

## Gradle (Kotlin DSL)

```kotlin
dependencies {
    testImplementation(platform("com.agenteval:agenteval-bom:1.0.0"))
    testImplementation("com.agenteval:agenteval-junit5")
    testImplementation("com.agenteval:agenteval-judge")
}
```

## Gradle (Groovy DSL)

```groovy
dependencies {
    testImplementation platform('com.agenteval:agenteval-bom:1.0.0')
    testImplementation 'com.agenteval:agenteval-junit5'
    testImplementation 'com.agenteval:agenteval-judge'
}
```

## Module Reference

| Module | Purpose | Required |
|---|---|---|
| `agenteval-core` | Test case model, metric interfaces, scoring engine | Transitive |
| `agenteval-metrics` | All built-in metric implementations | Transitive |
| `agenteval-judge` | LLM-as-judge engine and provider integrations | For LLM metrics |
| `agenteval-junit5` | JUnit 5 extension, annotations, assertion API | For test-suite use |
| `agenteval-datasets` | Dataset loading and management | Optional |
| `agenteval-reporting` | Console, XML, JSON, HTML reports | Optional |
| `agenteval-embeddings` | Embedding model integrations | For semantic metrics |
| `agenteval-spring-ai` | Spring AI auto-capture | Optional |
| `agenteval-langchain4j` | LangChain4j auto-capture | Optional |
| `agenteval-langgraph4j` | LangGraph4j graph execution capture | Optional |
| `agenteval-mcp` | MCP tool call capture | Optional |
| `agenteval-redteam` | Adversarial testing | Optional |

## Java Version

AgentEval requires **Java 21+** and makes use of:

- Records and sealed interfaces
- Pattern matching
- Virtual threads (for parallel evaluation)

## API Keys

Set environment variables for your judge provider before running evaluations:

```bash
# OpenAI
export OPENAI_API_KEY=sk-...

# Anthropic
export ANTHROPIC_API_KEY=sk-ant-...

# Google
export GOOGLE_API_KEY=...
```

See [Configuration](./configuration) for all options.
