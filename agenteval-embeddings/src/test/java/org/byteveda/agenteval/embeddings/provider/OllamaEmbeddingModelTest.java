package org.byteveda.agenteval.embeddings.provider;

import org.byteveda.agenteval.embeddings.EmbeddingException;
import org.byteveda.agenteval.embeddings.config.EmbeddingConfig;
import org.byteveda.agenteval.embeddings.http.HttpEmbeddingClient;
import org.byteveda.agenteval.embeddings.http.HttpEmbeddingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OllamaEmbeddingModelTest {

    private HttpEmbeddingClient client;
    private EmbeddingConfig config;

    @BeforeEach
    void setUp() {
        client = mock(HttpEmbeddingClient.class);
        config = EmbeddingConfig.builder()
                .model("nomic-embed-text")
                .baseUrl("http://localhost:11434")
                .build();
    }

    @Test
    void shouldParseEmbeddingResponse() {
        String responseBody = """
                {"embedding":[0.4,0.5,0.6]}
                """;
        when(client.send(any())).thenReturn(
                new HttpEmbeddingResponse(200, responseBody));

        var model = new OllamaEmbeddingModel(config, client);
        List<Double> embedding = model.embed("hello");

        assertThat(embedding).hasSize(3);
        assertThat(embedding.get(0)).isCloseTo(0.4, within(0.001));
        assertThat(embedding.get(1)).isCloseTo(0.5, within(0.001));
        assertThat(embedding.get(2)).isCloseTo(0.6, within(0.001));
    }

    @Test
    void shouldThrowOnErrorResponse() {
        when(client.send(any())).thenReturn(
                new HttpEmbeddingResponse(500, "Internal error"));

        var model = new OllamaEmbeddingModel(config, client);

        assertThatThrownBy(() -> model.embed("hello"))
                .isInstanceOf(EmbeddingException.class);
    }

    @Test
    void shouldReturnModelId() {
        var model = new OllamaEmbeddingModel(config, client);
        assertThat(model.modelId()).isEqualTo("nomic-embed-text");
    }

    @Test
    void shouldRejectNullText() {
        var model = new OllamaEmbeddingModel(config, client);
        assertThatThrownBy(() -> model.embed(null))
                .isInstanceOf(NullPointerException.class);
    }
}
