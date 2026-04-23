package org.byteveda.agenteval.spring.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SpringAiCaptureTest {

    @Test
    void rejectsNullChatModel() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SpringAiCapture(null))
                .withMessageContaining("chatModel");
    }

    @Test
    void forwardsPromptToChatModelAndPopulatesTestCase() {
        AtomicReference<Prompt> received = new AtomicReference<>();
        ChatModel stub = prompt -> {
            received.set(prompt);
            return new ChatResponse(
                    List.of(new Generation(new AssistantMessage("hello there"))),
                    ChatResponseMetadata.builder().usage(new DefaultUsage(1, 2, 3)).build());
        };

        var capture = new SpringAiCapture(stub);
        var testCase = capture.call("hi");

        assertThat(received.get()).isNotNull();
        assertThat(received.get().getContents()).contains("hi");
        assertThat(testCase.getInput()).isEqualTo("hi");
        assertThat(testCase.getActualOutput()).isEqualTo("hello there");
        assertThat(testCase.getTokenUsage()).isNotNull();
        assertThat(testCase.getLatencyMs()).isGreaterThanOrEqualTo(0L);
    }
}
