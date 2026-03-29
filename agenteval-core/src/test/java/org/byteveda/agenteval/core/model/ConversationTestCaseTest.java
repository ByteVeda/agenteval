package org.byteveda.agenteval.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationTestCaseTest {

    @Test
    void shouldBuildConversationTestCase() {
        var turn1 = AgentTestCase.builder()
                .input("Hello")
                .actualOutput("Hi there!")
                .build();
        var turn2 = AgentTestCase.builder()
                .input("What is your name?")
                .actualOutput("I'm an AI assistant.")
                .build();

        var conversation = ConversationTestCase.builder()
                .turns(List.of(turn1, turn2))
                .conversationId("conv-1")
                .systemPrompt("You are a helpful assistant.")
                .build();

        assertThat(conversation.getTurns()).hasSize(2);
        assertThat(conversation.getConversationId()).isEqualTo("conv-1");
        assertThat(conversation.getSystemPrompt()).isEqualTo("You are a helpful assistant.");
    }

    @Test
    void shouldRejectNullTurns() {
        assertThatThrownBy(() -> ConversationTestCase.builder().build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectEmptyTurns() {
        assertThatThrownBy(() -> ConversationTestCase.builder()
                .turns(List.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void shouldAllowNullOptionalFields() {
        var turn = AgentTestCase.builder().input("test").build();
        var conversation = ConversationTestCase.builder()
                .turns(List.of(turn))
                .build();

        assertThat(conversation.getConversationId()).isNull();
        assertThat(conversation.getSystemPrompt()).isNull();
    }

    @Test
    void shouldDefensiveCopyTurns() {
        var turn = AgentTestCase.builder().input("test").build();
        var turns = new java.util.ArrayList<>(List.of(turn));

        var conversation = ConversationTestCase.builder()
                .turns(turns)
                .build();

        turns.add(AgentTestCase.builder().input("extra").build());
        assertThat(conversation.getTurns()).hasSize(1);
    }
}
