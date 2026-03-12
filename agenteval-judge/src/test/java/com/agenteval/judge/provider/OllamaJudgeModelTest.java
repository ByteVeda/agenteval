package com.agenteval.judge.provider;

import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.judge.config.JudgeConfig;
import com.agenteval.judge.http.HttpJudgeClient;
import com.agenteval.judge.http.HttpJudgeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaJudgeModelTest {

    private HttpJudgeClient client;
    private JudgeConfig config;

    @BeforeEach
    void setUp() {
        client = mock(HttpJudgeClient.class);
        config = JudgeConfig.builder()
                .model("llama3")
                .baseUrl("http://localhost:11434")
                .build();
    }

    @Test
    void shouldParseOllamaResponse() {
        String responseBody = """
                {
                  "message": {"role": "assistant", "content": "{\\"score\\": 0.85, \\"reason\\": \\"Good answer\\"}"},
                  "prompt_eval_count": 50,
                  "eval_count": 100
                }
                """;
        when(client.send(any())).thenReturn(
                new HttpJudgeResponse(200, responseBody, null));

        var model = new OllamaJudgeModel(config, client);
        JudgeResponse response = model.judge("test prompt");

        assertThat(response.score()).isCloseTo(0.85, within(0.001));
        assertThat(response.reason()).isEqualTo("Good answer");
        assertThat(response.tokenUsage()).isNotNull();
        assertThat(response.tokenUsage().inputTokens()).isEqualTo(50);
        assertThat(response.tokenUsage().outputTokens()).isEqualTo(100);
    }

    @Test
    void shouldHandleMissingTokenUsage() {
        String responseBody = """
                {"message": {"role": "assistant", "content": "{\\"score\\": 0.5, \\"reason\\": \\"OK\\"}"}}
                """;
        when(client.send(any())).thenReturn(
                new HttpJudgeResponse(200, responseBody, null));

        var model = new OllamaJudgeModel(config, client);
        JudgeResponse response = model.judge("test");

        assertThat(response.score()).isCloseTo(0.5, within(0.001));
        assertThat(response.tokenUsage()).isNull();
    }

    @Test
    void shouldReturnModelId() {
        var model = new OllamaJudgeModel(config, client);
        assertThat(model.modelId()).isEqualTo("llama3");
    }

    @Test
    void shouldNotRequireApiKey() {
        var noKeyConfig = JudgeConfig.builder()
                .model("llama3")
                .baseUrl("http://localhost:11434")
                .build();
        var model = new OllamaJudgeModel(noKeyConfig, client);
        assertThat(model.modelId()).isEqualTo("llama3");
    }
}
