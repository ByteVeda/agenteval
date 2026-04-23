package org.byteveda.agenteval.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LangChain4jTestCaseBuilderTest {

    @Test
    void rejectsNullInput() {
        Response<AiMessage> response = Response.from(AiMessage.from("hello"));
        assertThatNullPointerException()
                .isThrownBy(() -> LangChain4jTestCaseBuilder.fromResponse(null, response))
                .withMessageContaining("input");
    }

    @Test
    void rejectsNullResponse() {
        assertThatNullPointerException()
                .isThrownBy(() -> LangChain4jTestCaseBuilder.fromResponse("q", null))
                .withMessageContaining("response");
    }

    @Test
    void copiesTextFromAiMessageIntoActualOutput() {
        Response<AiMessage> response = Response.from(AiMessage.from("the answer is 42"));

        var testCase = LangChain4jTestCaseBuilder.fromResponse("q?", response);

        assertThat(testCase.getInput()).isEqualTo("q?");
        assertThat(testCase.getActualOutput()).isEqualTo("the answer is 42");
        assertThat(testCase.getToolCalls()).isEmpty();
        assertThat(testCase.getTokenUsage()).isNull();
    }

    @Test
    void capturesToolExecutionRequestsAsToolCalls() {
        var toolReq = ToolExecutionRequest.builder()
                .id("t1")
                .name("search")
                .arguments("{\"query\":\"java\"}")
                .build();
        Response<AiMessage> response = Response.from(AiMessage.from("ok", List.of(toolReq)));

        var testCase = LangChain4jTestCaseBuilder.fromResponse("find java", response);

        assertThat(testCase.getToolCalls()).hasSize(1);
        var call = testCase.getToolCalls().get(0);
        assertThat(call.name()).isEqualTo("search");
        assertThat(call.arguments()).containsEntry("arguments", "{\"query\":\"java\"}");
    }

    @Test
    void capturesTokenUsageWhenPresent() {
        Response<AiMessage> response = Response.from(
                AiMessage.from("done"),
                new TokenUsage(12, 34, 46));

        var testCase = LangChain4jTestCaseBuilder.fromResponse("q", response);

        assertThat(testCase.getTokenUsage()).isNotNull();
        assertThat(testCase.getTokenUsage().inputTokens()).isEqualTo(12);
        assertThat(testCase.getTokenUsage().outputTokens()).isEqualTo(34);
        assertThat(testCase.getTokenUsage().totalTokens()).isEqualTo(46);
    }
}
