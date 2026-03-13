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

class GoogleJudgeModelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JudgeConfig config;
    private HttpJudgeClient mockClient;

    @BeforeEach
    void setUp() {
        config = JudgeConfig.builder()
                .apiKey("test-google-key")
                .model("gemini-1.5-pro")
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        mockClient = mock(HttpJudgeClient.class);
    }

    @Test
    void shouldReturnModelId() {
        var model = new GoogleJudgeModel(config, mockClient);
        assertThat(model.modelId()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void shouldBuildValidRequest() {
        var model = new GoogleJudgeModel(config, mockClient);

        when(mockClient.send(any())).thenAnswer(invocation -> {
            HttpJudgeRequest req = invocation.getArgument(0);
            assertThat(req.url()).isEqualTo(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent");
            assertThat(req.headers()).containsEntry("x-goog-api-key", "test-google-key");

            JsonNode body = MAPPER.readTree(req.body());
            assertThat(body.has("systemInstruction")).isTrue();
            assertThat(body.path("systemInstruction").path("parts").get(0)
                    .path("text").asText()).contains("evaluation judge");
            assertThat(body.path("contents").get(0).path("role").asText()).isEqualTo("user");
            assertThat(body.path("generationConfig").path("responseMimeType").asText())
                    .isEqualTo("application/json");

            return makeResponse(0.85, "Relevant");
        });

        model.judge("Evaluate this");
    }

    @Test
    void shouldParseSuccessResponse() {
        var model = new GoogleJudgeModel(config, mockClient);
        when(mockClient.send(any())).thenReturn(makeResponse(0.85, "Good answer"));

        JudgeResponse response = model.judge("test prompt");

        assertThat(response.score()).isCloseTo(0.85, within(0.001));
        assertThat(response.reason()).isEqualTo("Good answer");
        assertThat(response.tokenUsage()).isNotNull();
        assertThat(response.tokenUsage().inputTokens()).isEqualTo(120);
        assertThat(response.tokenUsage().outputTokens()).isEqualTo(45);
    }

    @Test
    void shouldHandleResponseWithoutUsage() {
        var model = new GoogleJudgeModel(config, mockClient);
        String responseBody = """
                {"candidates": [{"content": {"parts": [{"text": "{\\"score\\": 0.7, \\"reason\\": \\"OK\\"}"}]}}]}""";
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, responseBody, null));

        JudgeResponse response = model.judge("test");
        assertThat(response.score()).isCloseTo(0.7, within(0.001));
        assertThat(response.tokenUsage()).isNull();
    }

    @Test
    void shouldThrowOnEmptyCandidates() {
        var model = new GoogleJudgeModel(config, mockClient);
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, "{\"candidates\": []}", null));

        assertThatThrownBy(() -> model.judge("test"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("No candidates");
    }

    @Test
    void shouldThrowOnMissingApiKey() {
        var noKeyConfig = JudgeConfig.builder()
                .model("gemini-1.5-pro")
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        assertThatThrownBy(() -> new GoogleJudgeModel(noKeyConfig))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("Google requires");
    }

    private HttpJudgeResponse makeResponse(double score, String reason) {
        String content = String.format(
                "{\"score\": %s, \"reason\": \"%s\"}", score, reason);
        String body = String.format("""
                {
                  "candidates": [{"content": {"parts": [{"text": %s}]}}],
                  "usageMetadata": {
                    "promptTokenCount": 120,
                    "candidatesTokenCount": 45,
                    "totalTokenCount": 165
                  }
                }""", MAPPER.valueToTree(content));
        return new HttpJudgeResponse(200, body, null);
    }
}
