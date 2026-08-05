package org.byteveda.agenteval.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTestCaseTest {

    @Test
    void shouldBuildMinimalTestCase() {
        var tc = AgentTestCase.builder()
                .input("What is the refund policy?")
                .build();

        assertThat(tc.getInput()).isEqualTo("What is the refund policy?");
        assertThat(tc.getActualOutput()).isNull();
        assertThat(tc.getExpectedOutput()).isNull();
        assertThat(tc.getRetrievalContext()).isEmpty();
        assertThat(tc.getContext()).isEmpty();
        assertThat(tc.getToolCalls()).isEmpty();
        assertThat(tc.getExpectedToolCalls()).isEmpty();
        assertThat(tc.getReasoningTrace()).isEmpty();
        assertThat(tc.getLatencyMs()).isZero();
        assertThat(tc.getTokenUsage()).isNull();
        assertThat(tc.getCost()).isNull();
        assertThat(tc.getMetadata()).isEmpty();
    }

    @Test
    void shouldBuildFullTestCase() {
        var tc = AgentTestCase.builder()
                .input("How do I get a refund?")
                .actualOutput("You can request a refund within 30 days.")
                .expectedOutput("Full refund within 30 days of purchase.")
                .retrievalContext(List.of("doc1", "doc2"))
                .context(List.of("ground truth"))
                .toolCalls(List.of(ToolCall.of("search")))
                .expectedToolCalls(List.of(ToolCall.of("search")))
                .reasoningTrace(List.of(ReasoningStep.of(ReasoningStepType.PLAN, "search docs")))
                .latencyMs(250)
                .tokenUsage(TokenUsage.of(100, 50))
                .cost(new BigDecimal("0.005"))
                .metadata(Map.of("category", "refund"))
                .build();

        assertThat(tc.getInput()).isEqualTo("How do I get a refund?");
        assertThat(tc.getActualOutput()).isEqualTo("You can request a refund within 30 days.");
        assertThat(tc.getExpectedOutput()).isEqualTo("Full refund within 30 days of purchase.");
        assertThat(tc.getRetrievalContext()).hasSize(2);
        assertThat(tc.getContext()).hasSize(1);
        assertThat(tc.getToolCalls()).hasSize(1);
        assertThat(tc.getExpectedToolCalls()).hasSize(1);
        assertThat(tc.getReasoningTrace()).hasSize(1);
        assertThat(tc.getLatencyMs()).isEqualTo(250);
        assertThat(tc.getTokenUsage().totalTokens()).isEqualTo(150);
        assertThat(tc.getCost()).isEqualByComparingTo("0.005");
        assertThat(tc.getMetadata()).containsEntry("category", "refund");
    }

    @Test
    void shouldRejectNullInput() {
        assertThatThrownBy(() -> AgentTestCase.builder().build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("input");
    }

    @Test
    void shouldAllowMutableActualOutput() {
        var tc = AgentTestCase.builder()
                .input("test")
                .build();

        assertThat(tc.getActualOutput()).isNull();
        tc.setActualOutput("response from agent");
        assertThat(tc.getActualOutput()).isEqualTo("response from agent");
    }

    @Test
    void shouldAllowMutableLatency() {
        var tc = AgentTestCase.builder().input("test").build();
        tc.setLatencyMs(500);
        assertThat(tc.getLatencyMs()).isEqualTo(500);
    }

    @Test
    void shouldAllowMutableTokenUsage() {
        var tc = AgentTestCase.builder().input("test").build();
        tc.setTokenUsage(TokenUsage.of(200, 100));
        assertThat(tc.getTokenUsage().totalTokens()).isEqualTo(300);
    }

    @Test
    void shouldAllowMutableCost() {
        var tc = AgentTestCase.builder().input("test").build();
        tc.setCost(new BigDecimal("0.01"));
        assertThat(tc.getCost()).isEqualByComparingTo("0.01");
    }

    @Test
    void shouldDefensiveCopyLists() {
        var context = new java.util.ArrayList<>(List.of("doc1"));
        var tc = AgentTestCase.builder()
                .input("test")
                .retrievalContext(context)
                .build();

        context.add("doc2");
        assertThat(tc.getRetrievalContext()).hasSize(1);
    }

    @Test
    void shouldDefensiveCopyMetadata() {
        var meta = new java.util.HashMap<String, Object>();
        meta.put("key", "value");
        var tc = AgentTestCase.builder()
                .input("test")
                .metadata(meta)
                .build();

        meta.put("other", "val");
        assertThat(tc.getMetadata()).hasSize(1);
    }
}
