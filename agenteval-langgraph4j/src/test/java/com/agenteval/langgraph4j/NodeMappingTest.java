package com.agenteval.langgraph4j;

import com.agenteval.core.model.ReasoningStepType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeMappingTest {

    @Test
    void shouldMapConfiguredNodes() {
        var mapping = NodeMapping.builder()
                .map("planner", ReasoningStepType.PLAN)
                .map("executor", ReasoningStepType.ACTION)
                .build();

        assertThat(mapping.typeFor("planner")).isEqualTo(ReasoningStepType.PLAN);
        assertThat(mapping.typeFor("executor")).isEqualTo(ReasoningStepType.ACTION);
    }

    @Test
    void shouldReturnDefaultForUnmappedNodes() {
        var mapping = NodeMapping.builder()
                .defaultType(ReasoningStepType.OBSERVATION)
                .build();

        assertThat(mapping.typeFor("unknown")).isEqualTo(ReasoningStepType.OBSERVATION);
    }

    @Test
    void defaultMappingShouldMapToThought() {
        var mapping = NodeMapping.defaults();
        assertThat(mapping.typeFor("any")).isEqualTo(ReasoningStepType.THOUGHT);
    }
}
