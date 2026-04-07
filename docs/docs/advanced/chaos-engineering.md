---
sidebar_position: 5
---

# Chaos Engineering

The `agenteval-chaos` module injects controlled failures into agent evaluations to measure resilience. It answers the question: "When things go wrong, does my agent degrade gracefully?"

## Dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-chaos</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

## Chaos Categories

Six categories of failure can be injected:

| Category | Description |
|---|---|
| `TOOL_FAILURE` | Simulates tool/API call failures (unavailable, timeout, auth errors) |
| `CONTEXT_CORRUPTION` | Corrupts retrieval context (missing, contradictory, shuffled) |
| `LATENCY` | Simulates high-latency responses from tools |
| `SCHEMA_MUTATION` | Mutates tool response schemas unexpectedly |
| `CASCADING_FAILURE` | Simulates cascading failures across dependent services |
| `RESOURCE_EXHAUSTION` | Simulates rate limits and resource exhaustion |

## ChaosSuite Usage

`ChaosSuite` is the main entry point. Configure it with an agent function, a judge model for evaluating resilience, and the categories to test:

```java
var result = ChaosSuite.builder()
    .agent(input -> myAgent.respond(input))
    .judgeModel(judge)
    .categories(ChaosCategory.TOOL_FAILURE, ChaosCategory.CONTEXT_CORRUPTION)
    .build()
    .run();
```

If you omit `.categories(...)`, all six categories are included by default.

The `run()` method executes all built-in scenarios for the selected categories, calls the agent with each chaos-injected input, and uses the judge to evaluate whether the agent handled the failure gracefully.

## Injectors

Each chaos scenario uses a `ChaosInjector` to modify the test case before it reaches the agent. The `ChaosInjector` interface is sealed with four implementations:

### ToolFailureInjector

Replaces tool responses with error messages. Built-in scenarios cover:

- `"ERROR: Tool unavailable"`
- `"ERROR: Connection timeout"`
- `"ERROR: Service returned 500 Internal Server Error"`
- `"ERROR: Authentication failed"`

### ContextCorruptionInjector

Corrupts retrieval context using one of three modes:

- `CorruptionMode.MISSING` -- removes all retrieval context
- `CorruptionMode.CONTRADICTORY` -- injects contradictory information
- `CorruptionMode.SHUFFLED` -- shuffles context entries out of order

### LatencyInjector

Simulates delayed tool responses. Built-in scenarios:

- 5-second delay (high latency)
- 30-second delay (extreme latency)

### SchemaMutationInjector

Mutates tool response schemas using one of three strategies:

- `MutationType.WRAP_IN_ENVELOPE` -- wraps results in an unexpected JSON envelope
- `MutationType.TRUNCATE` -- truncates results mid-response
- `MutationType.NEST_IN_DATA` -- nests results in an unexpected data structure

## ChaosResult Interpretation

The `run()` method returns a `ChaosResult` record:

```java
ChaosResult result = suite.run();

// Overall resilience score (0.0 to 1.0)
double overall = result.overallScore();

// Fraction of scenarios where the agent was resilient
double rate = result.resilienceRate();

// Per-category average scores
Map<ChaosCategory, Double> byCategory = result.categoryScores();

// Individual scenario details
for (ChaosResult.ScenarioResult sr : result.results()) {
    System.out.printf("[%s] %s: score=%.2f resilient=%s%n",
        sr.category(), sr.scenarioName(),
        sr.score(), sr.resilient());
}
```

A scenario is considered resilient if its judge score meets or exceeds the threshold of 0.7. The overall score is the average across all scenario scores.

Each `ScenarioResult` contains:

| Field | Description |
|---|---|
| `category()` | The `ChaosCategory` |
| `scenarioName()` | Name of the scenario (e.g., `"tool-unavailable"`) |
| `input()` | The chaos-injected input sent to the agent |
| `response()` | The agent's response |
| `score()` | Resilience score from the judge (0.0--1.0) |
| `reason()` | Explanation from the judge |
| `resilient()` | Whether the agent handled the failure gracefully |

## Built-in Scenarios

The `ChaosScenarioLibrary` provides pre-built scenarios for every category. Use `getScenarios(ChaosCategory)` to retrieve scenarios for a specific category, or `getAllScenarios()` for the complete set.

| Category | Scenarios |
|---|---|
| `TOOL_FAILURE` | `tool-unavailable`, `tool-timeout`, `tool-server-error`, `tool-auth-failure` |
| `CONTEXT_CORRUPTION` | `context-missing`, `context-contradictory`, `context-shuffled` |
| `LATENCY` | `high-latency` (5s), `extreme-latency` (30s) |
| `SCHEMA_MUTATION` | `schema-envelope`, `schema-truncated`, `schema-nested` |
| `CASCADING_FAILURE` | `cascading-primary-down` |
| `RESOURCE_EXHAUSTION` | `rate-limited` |
