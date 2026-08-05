package org.byteveda.agenteval.mcp;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpTestCaseBuilderTest {

    @Test
    void shouldBuildTestCaseFromToolCalls() {
        var toolCalls = List.of(
                new ToolCall("search", Map.of("q", "java"), "results", 100));

        AgentTestCase tc = McpTestCaseBuilder.build("query", "answer", toolCalls);

        assertThat(tc.getInput()).isEqualTo("query");
        assertThat(tc.getActualOutput()).isEqualTo("answer");
        assertThat(tc.getToolCalls()).hasSize(1);
        assertThat(tc.getToolCalls().getFirst().name()).isEqualTo("search");
        assertThat(tc.getMetadata()).containsEntry("framework", "mcp");
    }

    @Test
    void shouldBuildFromCapture() {
        var capture = new McpCapture();
        capture.recordCall("tool1", Map.of("a", "b"), "result1", 50);

        AgentTestCase tc = McpTestCaseBuilder.fromCapture(
                "input", "output", capture);

        assertThat(tc.getToolCalls()).hasSize(1);
        assertThat(tc.getToolCalls().getFirst().name()).isEqualTo("tool1");
    }
}
