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

class BedrockJudgeModelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JudgeConfig config;
    private HttpJudgeClient mockClient;

    @BeforeEach
    void setUp() {
        config = JudgeConfig.builder()
                .model("anthropic.claude-3-sonnet-20240229-v1:0")
                .baseUrl("https://bedrock-runtime.us-east-1.amazonaws.com")
                .build();
        mockClient = mock(HttpJudgeClient.class);
    }

    @Test
    void shouldReturnModelId() {
        var model = new BedrockJudgeModel(config, "AKID", "secret", null, mockClient);
        assertThat(model.modelId()).isEqualTo("anthropic.claude-3-sonnet-20240229-v1:0");
    }

    @Test
    void shouldBuildValidRequest() {
        var model = new BedrockJudgeModel(config, "AKID", "secret", null, mockClient);

        when(mockClient.send(any())).thenAnswer(invocation -> {
            HttpJudgeRequest req = invocation.getArgument(0);
            assertThat(req.url()).isEqualTo(
                    "https://bedrock-runtime.us-east-1.amazonaws.com"
                            + "/model/anthropic.claude-3-sonnet-20240229-v1:0/invoke");
            assertThat(req.headers()).containsKey("Authorization");
            assertThat(req.headers().get("Authorization")).startsWith("AWS4-HMAC-SHA256");
            assertThat(req.headers()).containsKey("x-amz-date");
            assertThat(req.headers()).containsKey("x-amz-content-sha256");

            JsonNode body = MAPPER.readTree(req.body());
            assertThat(body.get("anthropic_version").asText()).isEqualTo("bedrock-2023-05-31");
            assertThat(body.get("max_tokens").asInt()).isEqualTo(1024);
            assertThat(body.has("system")).isTrue();
            assertThat(body.get("messages")).hasSize(1);

            return makeResponse(0.9, "Excellent");
        });

        model.judge("Evaluate this");
    }

    @Test
    void shouldParseSuccessResponse() {
        var model = new BedrockJudgeModel(config, "AKID", "secret", null, mockClient);
        when(mockClient.send(any())).thenReturn(makeResponse(0.9, "Excellent"));

        JudgeResponse response = model.judge("test prompt");

        assertThat(response.score()).isCloseTo(0.9, within(0.001));
        assertThat(response.reason()).isEqualTo("Excellent");
        assertThat(response.tokenUsage()).isNotNull();
        assertThat(response.tokenUsage().inputTokens()).isEqualTo(150);
        assertThat(response.tokenUsage().outputTokens()).isEqualTo(60);
    }

    @Test
    void shouldIncludeSessionToken() {
        var model = new BedrockJudgeModel(config, "AKID", "secret", "token123", mockClient);

        when(mockClient.send(any())).thenAnswer(invocation -> {
            HttpJudgeRequest req = invocation.getArgument(0);
            assertThat(req.headers()).containsEntry("x-amz-security-token", "token123");
            return makeResponse(0.8, "OK");
        });

        model.judge("test");
    }

    @Test
    void shouldExtractRegionFromBaseUrl() {
        assertThat(BedrockJudgeModel.extractRegion(
                "https://bedrock-runtime.eu-west-1.amazonaws.com"))
                .isEqualTo("eu-west-1");
        assertThat(BedrockJudgeModel.extractRegion(
                "https://bedrock-runtime.ap-southeast-1.amazonaws.com"))
                .isEqualTo("ap-southeast-1");
    }

    @Test
    void shouldDefaultRegionOnInvalidUrl() {
        assertThat(BedrockJudgeModel.extractRegion("http://localhost"))
                .isEqualTo("us-east-1");
    }

    @Test
    void shouldThrowOnEmptyContent() {
        var model = new BedrockJudgeModel(config, "AKID", "secret", null, mockClient);
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, "{\"content\": []}", null));

        assertThatThrownBy(() -> model.judge("test"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("No content");
    }

    @Test
    void shouldThrowOnMissingAccessKey() {
        assertThatThrownBy(() ->
                new BedrockJudgeModel(config, null, "secret", null))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("AWS_ACCESS_KEY_ID");
    }

    @Test
    void shouldThrowOnMissingSecretKey() {
        assertThatThrownBy(() ->
                new BedrockJudgeModel(config, "AKID", null, null))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("AWS_SECRET_ACCESS_KEY");
    }

    @Test
    void shouldHandleResponseWithoutUsage() {
        var model = new BedrockJudgeModel(config, "AKID", "secret", null, mockClient);
        String responseBody = """
                {"content": [{"type": "text", "text": "{\\"score\\": 0.7, \\"reason\\": \\"OK\\"}"}]}""";
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, responseBody, null));

        JudgeResponse response = model.judge("test");
        assertThat(response.score()).isCloseTo(0.7, within(0.001));
        assertThat(response.tokenUsage()).isNull();
    }

    private HttpJudgeResponse makeResponse(double score, String reason) {
        String content = String.format(
                "{\"score\": %s, \"reason\": \"%s\"}", score, reason);
        String body = String.format("""
                {
                  "content": [{"type": "text", "text": %s}],
                  "usage": {"input_tokens": 150, "output_tokens": 60}
                }""", MAPPER.valueToTree(content));
        return new HttpJudgeResponse(200, body, null);
    }
}
