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

class AnthropicJudgeModelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JudgeConfig config;
    private HttpJudgeClient mockClient;

    @BeforeEach
    void setUp() {
        config = JudgeConfig.builder()
                .apiKey("sk-ant-test")
                .model("claude-sonnet-4-20250514")
                .baseUrl("https://api.anthropic.com")
                .build();
        mockClient = mock(HttpJudgeClient.class);
    }

    @Test
    void shouldReturnModelId() {
        var model = new AnthropicJudgeModel(config, mockClient);
        assertThat(model.modelId()).isEqualTo("claude-sonnet-4-20250514");
    }

    @Test
    void shouldBuildValidRequest() {
        var model = new AnthropicJudgeModel(config, mockClient);

        when(mockClient.send(any())).thenAnswer(invocation -> {
            HttpJudgeRequest req = invocation.getArgument(0);
            assertThat(req.url()).isEqualTo("https://api.anthropic.com/v1/messages");
            assertThat(req.headers()).containsEntry("x-api-key", "sk-ant-test");
            assertThat(req.headers()).containsEntry("anthropic-version", "2023-06-01");

            JsonNode body = MAPPER.readTree(req.body());
            assertThat(body.get("model").asText()).isEqualTo("claude-sonnet-4-20250514");
            assertThat(body.get("max_tokens").asInt()).isEqualTo(1024);
            assertThat(body.has("system")).isTrue();

            return makeResponse(0.9, "Excellent");
        });

        model.judge("Evaluate this");
    }

    @Test
    void shouldParseSuccessResponse() {
        var model = new AnthropicJudgeModel(config, mockClient);
        when(mockClient.send(any())).thenReturn(makeResponse(0.9, "Excellent"));

        JudgeResponse response = model.judge("test prompt");

        assertThat(response.score()).isCloseTo(0.9, within(0.001));
        assertThat(response.reason()).isEqualTo("Excellent");
        assertThat(response.tokenUsage()).isNotNull();
        assertThat(response.tokenUsage().inputTokens()).isEqualTo(200);
        assertThat(response.tokenUsage().outputTokens()).isEqualTo(80);
    }

    @Test
    void shouldThrowOnEmptyContent() {
        var model = new AnthropicJudgeModel(config, mockClient);
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, "{\"content\": []}", null));

        assertThatThrownBy(() -> model.judge("test"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("No content");
    }

    @Test
    void shouldThrowOnNoTextBlock() {
        var model = new AnthropicJudgeModel(config, mockClient);
        String body = """
                {"content": [{"type": "image", "source": {}}]}""";
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, body, null));

        assertThatThrownBy(() -> model.judge("test"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("No text block");
    }

    private HttpJudgeResponse makeResponse(double score, String reason) {
        String content = String.format(
                "{\"score\": %s, \"reason\": \"%s\"}", score, reason);
        String body = String.format("""
                {
                  "content": [{"type": "text", "text": %s}],
                  "usage": {"input_tokens": 200, "output_tokens": 80}
                }""", MAPPER.valueToTree(content));
        return new HttpJudgeResponse(200, body, null);
    }
}
