package org.byteveda.agenteval.langchain4j;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LangChain4jCaptureTest {

    @Test
    void rejectsNullModel() {
        assertThatNullPointerException()
                .isThrownBy(() -> new LangChain4jCapture(null))
                .withMessageContaining("model");
    }

    @Test
    void forwardsInputToModelAndPopulatesTestCase() {
        AtomicReference<List<ChatMessage>> received = new AtomicReference<>();
        ChatLanguageModel stub = new ChatLanguageModel() {
            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                received.set(messages);
                return Response.from(AiMessage.from("hello there"), new TokenUsage(1, 2, 3));
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> tools) {
                return generate(messages);
            }
        };

        var capture = new LangChain4jCapture(stub);
        var testCase = capture.call("hi");

        assertThat(received.get()).hasSize(1);
        assertThat(received.get().get(0)).isInstanceOf(UserMessage.class);
        assertThat(testCase.getInput()).isEqualTo("hi");
        assertThat(testCase.getActualOutput()).isEqualTo("hello there");
        assertThat(testCase.getTokenUsage()).isNotNull();
        assertThat(testCase.getLatencyMs()).isGreaterThanOrEqualTo(0L);
    }
}
