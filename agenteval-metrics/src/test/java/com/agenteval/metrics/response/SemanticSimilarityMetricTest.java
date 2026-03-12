package com.agenteval.metrics.response;

import com.agenteval.core.embedding.EmbeddingModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("removal")
class SemanticSimilarityMetricTest {

    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.modelId()).thenReturn("test-model");
    }

    @Test
    void shouldScoreIdenticalEmbeddings() {
        when(embeddingModel.embed(anyString()))
                .thenReturn(List.of(1.0, 0.0, 0.0));

        var metric = new SemanticSimilarityMetric(embeddingModel, 0.7);
        var testCase = AgentTestCase.builder()
                .input("query")
                .actualOutput("same text")
                .expectedOutput("same text")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreOrthogonalEmbeddings() {
        when(embeddingModel.embed("actual")).thenReturn(List.of(1.0, 0.0, 0.0));
        when(embeddingModel.embed("expected")).thenReturn(List.of(0.0, 1.0, 0.0));

        var metric = new SemanticSimilarityMetric(embeddingModel, 0.7);
        var testCase = AgentTestCase.builder()
                .input("query")
                .actualOutput("actual")
                .expectedOutput("expected")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.0, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldRejectMissingActualOutput() {
        var metric = new SemanticSimilarityMetric(embeddingModel);
        var testCase = AgentTestCase.builder()
                .input("query")
                .expectedOutput("expected")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingExpectedOutput() {
        var metric = new SemanticSimilarityMetric(embeddingModel);
        var testCase = AgentTestCase.builder()
                .input("query")
                .actualOutput("actual")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new SemanticSimilarityMetric(embeddingModel);
        assertThat(metric.name()).isEqualTo("SemanticSimilarity");
    }

    @Test
    void shouldCalculateCosineSimilarity() {
        double sim = SemanticSimilarityMetric.cosineSimilarity(
                List.of(1.0, 0.0), List.of(1.0, 0.0));
        assertThat(sim).isCloseTo(1.0, within(0.001));

        sim = SemanticSimilarityMetric.cosineSimilarity(
                List.of(1.0, 0.0), List.of(0.0, 1.0));
        assertThat(sim).isCloseTo(0.0, within(0.001));

        sim = SemanticSimilarityMetric.cosineSimilarity(
                List.of(1.0, 1.0), List.of(1.0, 1.0));
        assertThat(sim).isCloseTo(1.0, within(0.001));
    }
}
