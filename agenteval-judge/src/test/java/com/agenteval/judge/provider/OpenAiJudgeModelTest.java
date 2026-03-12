package com.agenteval.judge.provider;

import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.judge.JudgeException;
import com.agenteval.judge.config.JudgeConfig;
import com.agenteval.judge.http.HttpJudgeClient;
import com.agenteval.judge.http.HttpJudgeRequest;
import com.agenteval.judge.http.HttpJudgeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiJudgeModelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JudgeConfig config;
    private HttpJudgeClient mockClient;

    @BeforeEach
    void setUp() {
        config = JudgeConfig.builder()
                .apiKey("sk-test")
                .model("gpt-4o")
                .baseUrl("https://api.openai.com")
                .build();
        mockClient = mock(HttpJudgeClient.class);
    }

    @Test
    void shouldReturnModelId() {
        var model = new OpenAiJudgeModel(config, mockClient);
        assertThat(model.modelId()).isEqualTo("gpt-4o");
    }

    @Test
    void shouldBuildValidRequest() {
        var model = new OpenAiJudgeModel(config, mockClient);

        when(mockClient.send(any())).thenAnswer(invocation -> {
            HttpJudgeRequest req = invocation.getArgument(0);
            assertThat(req.url()).isEqualTo("https://api.openai.com/v1/chat/completions");
            assertThat(req.headers()).containsKey("Authorization");
            assertThat(req.headers().get("Authorization")).isEqualTo("Bearer sk-test");

            JsonNode body = MAPPER.readTree(req.body());
            assertThat(body.get("model").asText()).isEqualTo("gpt-4o");
            assertThat(body.get("response_format").get("type").asText()).isEqualTo("json_object");
            assertThat(body.get("messages")).hasSize(2);

            return makeResponse(0.85, "Relevant", 100, 50, 150);
        });

        model.judge("Evaluate this");
    }

    @Test
    void shouldParseSuccessResponse() {
        var model = new OpenAiJudgeModel(config, mockClient);
        when(mockClient.send(any())).thenReturn(
                makeResponse(0.85, "Good answer", 100, 50, 150));

        JudgeResponse response = model.judge("test prompt");

        assertThat(response.score()).isCloseTo(0.85, within(0.001));
        assertThat(response.reason()).isEqualTo("Good answer");
        assertThat(response.tokenUsage()).isNotNull();
        assertThat(response.tokenUsage().inputTokens()).isEqualTo(100);
        assertThat(response.tokenUsage().outputTokens()).isEqualTo(50);
    }

    @Test
    void shouldHandleResponseWithoutUsage() {
        var model = new OpenAiJudgeModel(config, mockClient);
        String responseBody = """
                {"choices": [{"message": {"content": "{\\"score\\": 0.7, \\"reason\\": \\"OK\\"}"}}]}""";
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, responseBody, null));

        JudgeResponse response = model.judge("test");
        assertThat(response.score()).isCloseTo(0.7, within(0.001));
        assertThat(response.tokenUsage()).isNull();
    }

    @Test
    void shouldThrowOnEmptyChoices() {
        var model = new OpenAiJudgeModel(config, mockClient);
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, "{\"choices\": []}", null));

        assertThatThrownBy(() -> model.judge("test"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("No choices");
    }

    private HttpJudgeResponse makeResponse(
            double score, String reason, int promptTokens, int completionTokens, int totalTokens) {
        String content = String.format(
                "{\"score\": %s, \"reason\": \"%s\"}", score, reason);
        String body = String.format("""
                {
                  "choices": [{"message": {"content": %s}}],
                  "usage": {
                    "prompt_tokens": %d,
                    "completion_tokens": %d,
                    "total_tokens": %d
                  }
                }""",
                MAPPER.valueToTree(content), promptTokens, completionTokens, totalTokens);
        return new HttpJudgeResponse(200, body, null);
    }
}
