package org.byteveda.agenteval.spring.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SpringAiTestCaseBuilderTest {

    @Test
    void rejectsNullInput() {
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage("hi"))));
        assertThatNullPointerException()
                .isThrownBy(() -> SpringAiTestCaseBuilder.fromChatResponse(null, response))
                .withMessageContaining("input");
    }

    @Test
    void rejectsNullResponse() {
        assertThatNullPointerException()
                .isThrownBy(() -> SpringAiTestCaseBuilder.fromChatResponse("q", null))
                .withMessageContaining("response");
    }

    @Test
    void copiesAssistantMessageTextIntoActualOutput() {
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage("the answer is 42"))));

        var testCase = SpringAiTestCaseBuilder.fromChatResponse("q?", response);

        assertThat(testCase.getInput()).isEqualTo("q?");
        assertThat(testCase.getActualOutput()).isEqualTo("the answer is 42");
    }

    @Test
    void capturesTokenUsageFromMetadata() {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .usage(new DefaultUsage(12, 34, 46))
                .build();
        ChatResponse response = new ChatResponse(
                List.of(new Generation(new AssistantMessage("done"))),
                metadata);

        var testCase = SpringAiTestCaseBuilder.fromChatResponse("q", response);

        assertThat(testCase.getTokenUsage()).isNotNull();
        assertThat(testCase.getTokenUsage().inputTokens()).isEqualTo(12);
        assertThat(testCase.getTokenUsage().outputTokens()).isEqualTo(34);
        assertThat(testCase.getTokenUsage().totalTokens()).isEqualTo(46);
    }
}
