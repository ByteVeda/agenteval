---
sidebar_position: 3
---

# LangGraph4j Integration

Capture graph execution traces, node transitions, and state snapshots from LangGraph4j agents.

## Dependency

```xml
<dependency>
  <groupId>com.agenteval</groupId>
  <artifactId>agenteval-langgraph4j</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

## Setup

Attach the AgentEval graph observer to your LangGraph4j workflow:

```java
@ExtendWith(AgentEvalExtension.class)
class WorkflowAgentTest {

    private final GraphEvalCapture capture = new GraphEvalCapture();

    private final CompiledGraph<AgentState> graph = StateGraph.builder()
        .addNode("retrieve", retrieveNode)
        .addNode("generate", generateNode)
        .addEdge("retrieve", "generate")
        .observer(capture.graphObserver())    // attach capture
        .compile();

    @Test
    @AgentTest
    @Metric(value = TaskCompletion.class, threshold = 0.7)
    @Metric(value = TrajectoryOptimality.class, threshold = 0.6)
    void workflowShouldCompleteTask() {
        var output = graph.invoke(Map.of("input", "Summarize the Q3 report."));

        var testCase = capture.buildTestCase()
            .input("Summarize the Q3 report.")
            .actualOutput(output.get("output").toString())
            .build();
        // reasoningTrace is auto-populated from graph node transitions

        AgentAssertions.assertThat(testCase)
            .meetsMetric(new TaskCompletion(0.7))
            .meetsMetric(new TrajectoryOptimality(0.6));
    }
}
```

## What Gets Captured

| Field | Source |
|---|---|
| `reasoningTrace` | Graph node transitions mapped to `ReasoningStep` objects |
| `toolCalls` | Tool nodes extracted from graph execution |
| State snapshots | Available via `capture.getStateSnapshots()` |
