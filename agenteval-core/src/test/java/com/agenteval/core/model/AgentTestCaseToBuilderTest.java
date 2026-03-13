package com.agenteval.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTestCaseToBuilderTest {

    @Test
    void copiesAllFields() {
        AgentTestCase original = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .expectedOutput("expected")
                .retrievalContext(List.of("ctx1"))
                .context(List.of("doc1"))
                .toolCalls(List.of(new ToolCall("fn", Map.of("a", (Object) "b"), "result", 0L)))
                .expectedToolCalls(List.of(ToolCall.of("fn2")))
                .latencyMs(42L)
                .cost(BigDecimal.TEN)
                .metadata(Map.of("k", "v"))
                .build();

        AgentTestCase copy = original.toBuilder().build();

        assertThat(copy.getInput()).isEqualTo("question");
        assertThat(copy.getActualOutput()).isEqualTo("answer");
        assertThat(copy.getExpectedOutput()).isEqualTo("expected");
        assertThat(copy.getRetrievalContext()).containsExactly("ctx1");
        assertThat(copy.getContext()).containsExactly("doc1");
        assertThat(copy.getToolCalls()).hasSize(1);
        assertThat(copy.getExpectedToolCalls()).hasSize(1);
        assertThat(copy.getLatencyMs()).isEqualTo(42L);
        assertThat(copy.getCost()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(copy.getMetadata()).containsEntry("k", "v");
    }

    @Test
    void overrideFieldsOnCopy() {
        AgentTestCase original = AgentTestCase.builder()
                .input("q1")
                .actualOutput("a1")
                .build();

        AgentTestCase modified = original.toBuilder()
                .actualOutput("new answer")
                .build();

        assertThat(modified.getInput()).isEqualTo("q1");
        assertThat(modified.getActualOutput()).isEqualTo("new answer");
        assertThat(original.getActualOutput()).isEqualTo("a1");
    }

    @Test
    void copyIsIndependent() {
        AgentTestCase original = AgentTestCase.builder()
                .input("q1")
                .actualOutput("a1")
                .build();

        AgentTestCase copy = original.toBuilder().build();
        copy.setActualOutput("mutated");

        assertThat(original.getActualOutput()).isEqualTo("a1");
        assertThat(copy.getActualOutput()).isEqualTo("mutated");
    }

    @Test
    void emptyListFieldsHandled() {
        AgentTestCase original = AgentTestCase.builder()
                .input("q")
                .build();

        AgentTestCase copy = original.toBuilder().build();

        assertThat(copy.getRetrievalContext()).isEmpty();
        assertThat(copy.getContext()).isEmpty();
        assertThat(copy.getToolCalls()).isEmpty();
        assertThat(copy.getExpectedToolCalls()).isEmpty();
        assertThat(copy.getReasoningTrace()).isEmpty();
        assertThat(copy.getMetadata()).isEmpty();
    }
}
