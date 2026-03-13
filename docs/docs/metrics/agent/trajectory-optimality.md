---
sidebar_position: 7
---

# TrajectoryOptimality

Measures whether the agent took an efficient path to the solution. Penalizes unnecessary tool calls, redundant LLM invocations, and circular reasoning.

| Property | Value |
|---|---|
| Default threshold | 0.5 |
| Requires LLM judge | Yes |
| Required fields | `input`, `actualOutput`, `reasoningTrace` |
| Available since | P1 |

## Example

```java
// Optimal: 3 steps
var optimal = AgentTestCase.builder()
    .input("What is the weather in London?")
    .reasoningTrace(List.of(
        ReasoningStep.of(StepType.ACTION, "Calling GetWeather(city=London)"),
        ReasoningStep.of(StepType.OBSERVATION, "Weather: 12°C, cloudy"),
        ReasoningStep.of(StepType.ACTION, "Returning result to user")
    ))
    .build();

// score.value() → 0.95

// Inefficient: 7 steps for same task
var inefficient = AgentTestCase.builder()
    .input("What is the weather in London?")
    .reasoningTrace(List.of(
        ReasoningStep.of(StepType.THOUGHT, "I should search the web for London weather"),
        ReasoningStep.of(StepType.ACTION, "Calling WebSearch(query='London weather today')"),
        ReasoningStep.of(StepType.THOUGHT, "Let me try a more specific query"),
        ReasoningStep.of(StepType.ACTION, "Calling WebSearch(query='current weather London UK')"),
        ReasoningStep.of(StepType.ACTION, "Calling GetWeather(city=London)"),
        ReasoningStep.of(StepType.OBSERVATION, "12°C, cloudy"),
        ReasoningStep.of(StepType.ACTION, "Returning result")
    ))
    .build();

// score.value() → 0.35
// score.reason() → "Agent made 2 redundant web searches before calling the weather tool.
//                   Optimal trajectory requires 3 steps; actual: 7."
```

## In JUnit 5

```java
@Test
@AgentTest
@Metric(value = TrajectoryOptimality.class, threshold = 0.6)
void agentShouldTakeEfficientPath() {
    var testCase = AgentTestCase.builder()
        .input(task)
        .actualOutput(agent.run(task))
        .reasoningTrace(agent.getLastReasoningTrace())
        .build();

    AgentAssertions.assertThat(testCase)
        .meetsMetric(new TrajectoryOptimality(0.6));
}
```

## Configuration

| Option | Type | Default | Description |
|---|---|---|---|
| `threshold` | `double` | `0.5` | Minimum score to pass |
| `maxSteps` | `int` | null | Maximum acceptable steps; exceeding this reduces score |
