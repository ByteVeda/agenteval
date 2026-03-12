package com.agenteval.embeddings.provider;

import com.agenteval.embeddings.EmbeddingException;
import com.agenteval.embeddings.config.EmbeddingConfig;
import com.agenteval.embeddings.http.HttpEmbeddingClient;
import com.agenteval.embeddings.http.HttpEmbeddingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAiEmbeddingModelTest {

    private HttpEmbeddingClient client;
    private EmbeddingConfig config;

    @BeforeEach
    void setUp() {
        client = mock(HttpEmbeddingClient.class);
        config = EmbeddingConfig.builder()
                .apiKey("test-key")
                .model("text-embedding-3-small")
                .baseUrl("https://api.openai.com")
                .build();
    }

    @Test
    void shouldParseEmbeddingResponse() {
        String responseBody = """
                {"data":[{"embedding":[0.1,0.2,0.3]}],"model":"text-embedding-3-small"}
                """;
        when(client.send(any())).thenReturn(
                new HttpEmbeddingResponse(200, responseBody));

        var model = new OpenAiEmbeddingModel(config, client);
        List<Double> embedding = model.embed("hello");

        assertThat(embedding).hasSize(3);
        assertThat(embedding.get(0)).isCloseTo(0.1, within(0.001));
        assertThat(embedding.get(1)).isCloseTo(0.2, within(0.001));
        assertThat(embedding.get(2)).isCloseTo(0.3, within(0.001));
    }

    @Test
    void shouldThrowOnErrorResponse() {
        when(client.send(any())).thenReturn(
                new HttpEmbeddingResponse(401, "Unauthorized"));

        var model = new OpenAiEmbeddingModel(config, client);

        assertThatThrownBy(() -> model.embed("hello"))
                .isInstanceOf(EmbeddingException.class);
    }

    @Test
    void shouldReturnModelId() {
        var model = new OpenAiEmbeddingModel(config, client);
        assertThat(model.modelId()).isEqualTo("text-embedding-3-small");
    }

    @Test
    void shouldRejectNullText() {
        var model = new OpenAiEmbeddingModel(config, client);
        assertThatThrownBy(() -> model.embed(null))
                .isInstanceOf(NullPointerException.class);
    }
}
