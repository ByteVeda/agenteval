package com.agenteval.embeddings.provider;

import com.agenteval.embeddings.EmbeddingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CustomHttpEmbeddingModelTest {

    @Test
    void shouldRenderTemplateWithInput() {
        String template = "{\"text\": \"{{input}}\", \"model\": \"custom\"}";
        String result = CustomHttpEmbeddingModel.renderTemplate(template, "hello world");
        assertThat(result).isEqualTo("{\"text\": \"hello world\", \"model\": \"custom\"}");
    }

    @Test
    void shouldEscapeQuotesInInput() {
        String template = "{\"text\": \"{{input}}\"}";
        String result = CustomHttpEmbeddingModel.renderTemplate(template, "say \"hi\"");
        assertThat(result).isEqualTo("{\"text\": \"say \\\"hi\\\"\"}");
    }

    @Test
    void shouldParseEmbeddingFromSimplePath() {
        String json = "{\"embedding\": [0.1, 0.2, 0.3]}";
        List<Double> result = CustomHttpEmbeddingModel.parseEmbedding(json, "embedding");

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isCloseTo(0.1, within(0.001));
        assertThat(result.get(1)).isCloseTo(0.2, within(0.001));
        assertThat(result.get(2)).isCloseTo(0.3, within(0.001));
    }

    @Test
    void shouldParseEmbeddingFromNestedPath() {
        String json = "{\"data\": {\"result\": [0.5, 0.6]}}";
        List<Double> result = CustomHttpEmbeddingModel.parseEmbedding(json, "data.result");

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isCloseTo(0.5, within(0.001));
    }

    @Test
    void shouldParseEmbeddingWithArrayIndex() {
        String json = "{\"data\": [{\"embedding\": [0.1, 0.2]}]}";
        List<Double> result = CustomHttpEmbeddingModel.parseEmbedding(
                json, "data.0.embedding");

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isCloseTo(0.1, within(0.001));
    }

    @Test
    void shouldThrowOnMissingJsonPath() {
        String json = "{\"other\": [1.0]}";
        assertThatThrownBy(() ->
                CustomHttpEmbeddingModel.parseEmbedding(json, "data.embedding"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldThrowOnNonArrayResult() {
        String json = "{\"data\": \"not an array\"}";
        assertThatThrownBy(() ->
                CustomHttpEmbeddingModel.parseEmbedding(json, "data"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Expected array");
    }
}
