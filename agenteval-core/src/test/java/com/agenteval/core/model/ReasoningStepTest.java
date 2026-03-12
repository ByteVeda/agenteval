package com.agenteval.core.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReasoningStepTest {

    @Test
    void shouldCreateReasoningStep() {
        var step = new ReasoningStep(ReasoningStepType.THOUGHT, "Analyzing the query", null);

        assertThat(step.type()).isEqualTo(ReasoningStepType.THOUGHT);
        assertThat(step.content()).isEqualTo("Analyzing the query");
        assertThat(step.toolCall()).isNull();
    }

    @Test
    void ofShouldCreateStepWithoutToolCall() {
        var step = ReasoningStep.of(ReasoningStepType.PLAN, "Step 1: search docs");

        assertThat(step.type()).isEqualTo(ReasoningStepType.PLAN);
        assertThat(step.content()).isEqualTo("Step 1: search docs");
        assertThat(step.toolCall()).isNull();
    }

    @Test
    void actionShouldCreateActionStepWithToolCall() {
        var tc = ToolCall.of("search", Map.of("query", "refund"));
        var step = ReasoningStep.action("Calling search API", tc);

        assertThat(step.type()).isEqualTo(ReasoningStepType.ACTION);
        assertThat(step.content()).isEqualTo("Calling search API");
        assertThat(step.toolCall()).isNotNull();
        assertThat(step.toolCall().name()).isEqualTo("search");
    }

    @Test
    void shouldRejectNullType() {
        assertThatThrownBy(() -> new ReasoningStep(null, "content", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullContent() {
        assertThatThrownBy(() -> new ReasoningStep(ReasoningStepType.THOUGHT, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void allEnumValuesShouldExist() {
        assertThat(ReasoningStepType.values())
                .containsExactly(
                        ReasoningStepType.PLAN,
                        ReasoningStepType.THOUGHT,
                        ReasoningStepType.OBSERVATION,
                        ReasoningStepType.ACTION
                );
    }
}
