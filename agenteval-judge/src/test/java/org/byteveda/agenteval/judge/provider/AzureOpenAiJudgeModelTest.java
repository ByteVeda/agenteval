package org.byteveda.agenteval.judge.provider;

import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.judge.JudgeException;
import org.byteveda.agenteval.judge.config.JudgeConfig;
import org.byteveda.agenteval.judge.http.HttpJudgeClient;
import org.byteveda.agenteval.judge.http.HttpJudgeRequest;
import org.byteveda.agenteval.judge.http.HttpJudgeResponse;
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

class AzureOpenAiJudgeModelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JudgeConfig config;
    private HttpJudgeClient mockClient;

    @BeforeEach
    void setUp() {
        config = JudgeConfig.builder()
                .apiKey("azure-test-key")
                .model("my-gpt4-deployment")
                .baseUrl("https://myresource.openai.azure.com")
                .build();
        mockClient = mock(HttpJudgeClient.class);
    }

    @Test
    void shouldReturnModelId() {
        var model = new AzureOpenAiJudgeModel(config, "2024-02-01", mockClient);
        assertThat(model.modelId()).isEqualTo("my-gpt4-deployment");
    }

    @Test
    void shouldBuildValidRequest() {
        var model = new AzureOpenAiJudgeModel(config, "2024-02-01", mockClient);

        when(mockClient.send(any())).thenAnswer(invocation -> {
            HttpJudgeRequest req = invocation.getArgument(0);
            assertThat(req.url()).isEqualTo(
                    "https://myresource.openai.azure.com/openai/deployments/"
                            + "my-gpt4-deployment/chat/completions?api-version=2024-02-01");
            assertThat(req.headers()).containsEntry("api-key", "azure-test-key");
            assertThat(req.headers()).doesNotContainKey("Authorization");

            JsonNode body = MAPPER.readTree(req.body());
            assertThat(body.has("model")).isFalse();
            assertThat(body.get("response_format").get("type").asText()).isEqualTo("json_object");
            assertThat(body.get("messages")).hasSize(2);

            return makeResponse(0.85, "Relevant", 100, 50, 150);
        });

        model.judge("Evaluate this");
    }

    @Test
    void shouldParseSuccessResponse() {
        var model = new AzureOpenAiJudgeModel(config, "2024-02-01", mockClient);
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
    void shouldUseDefaultApiVersion() {
        var model = new AzureOpenAiJudgeModel(config, null, mockClient);
        assertThat(model.getApiVersion()).isEqualTo("2024-02-01");
    }

    @Test
    void shouldThrowOnEmptyChoices() {
        var model = new AzureOpenAiJudgeModel(config, "2024-02-01", mockClient);
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, "{\"choices\": []}", null));

        assertThatThrownBy(() -> model.judge("test"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("No choices");
    }

    @Test
    void shouldThrowOnMissingApiKey() {
        var noKeyConfig = JudgeConfig.builder()
                .model("my-deployment")
                .baseUrl("https://myresource.openai.azure.com")
                .build();

        assertThatThrownBy(() -> new AzureOpenAiJudgeModel(noKeyConfig))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("Azure OpenAI requires");
    }

    @Test
    void shouldHandleResponseWithoutUsage() {
        var model = new AzureOpenAiJudgeModel(config, "2024-02-01", mockClient);
        String responseBody = """
                {"choices": [{"message": {"content": "{\\"score\\": 0.7, \\"reason\\": \\"OK\\"}"}}]}""";
        when(mockClient.send(any())).thenReturn(
                new HttpJudgeResponse(200, responseBody, null));

        JudgeResponse response = model.judge("test");
        assertThat(response.score()).isCloseTo(0.7, within(0.001));
        assertThat(response.tokenUsage()).isNull();
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
