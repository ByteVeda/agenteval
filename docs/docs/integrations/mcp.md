---
sidebar_position: 4
---

# MCP Integration

Capture MCP (Model Context Protocol) tool calls from the MCP Java SDK.

## Dependency

```xml
<dependency>
  <groupId>org.byteveda.agenteval</groupId>
  <artifactId>agenteval-mcp</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

## Setup

```java
@ExtendWith(AgentEvalExtension.class)
class McpAgentTest {

    private final McpEvalCapture capture = new McpEvalCapture();

    private final McpClient mcpClient = McpClient.builder()
        .transport(StdioClientTransport.builder().build())
        .interceptor(capture.mcpInterceptor())   // attach capture
        .build();

    @Test
    @AgentTest
    @Metric(value = ToolSelectionAccuracy.class, threshold = 0.9)
    @Metric(value = ToolArgumentCorrectness.class, threshold = 0.9)
    void mcpToolCallTest() {
        String response = agent.runWithMcp(mcpClient, "What files are in the project?");

        var testCase = capture.buildTestCase()
            .input("What files are in the project?")
            .actualOutput(response)
            .expectedToolCalls(List.of(
                ToolCall.of("list_directory", Map.of("path", "."))
            ))
            .build();

        AgentAssertions.assertThat(testCase)
            .calledTool("list_directory")
            .meetsMetric(new ToolSelectionAccuracy(0.9));
    }
}
```

## What Gets Captured

| Field | Source |
|---|---|
| `toolCalls` | MCP `CallToolRequest` / `CallToolResult` pairs |
| Tool arguments | MCP request parameters |
| Tool results | MCP server responses |
