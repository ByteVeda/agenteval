package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.embedding.EmbeddingModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolResultUtilizationMetricTest {

    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.modelId()).thenReturn("test-model");
    }

    @Test
    void shouldScoreHighWhenOutputMatchesToolResults() {
        var sameEmb = List.of(1.0, 0.0, 0.0);
        when(embeddingModel.embed("The weather is sunny")).thenReturn(sameEmb);
        when(embeddingModel.embed("sunny, 25C")).thenReturn(sameEmb);

        var tc = AgentTestCase.builder()
                .input("What's the weather?")
                .actualOutput("The weather is sunny")
                .toolCalls(List.of(
                        new ToolCall("getWeather", Map.of(), "sunny, 25C", 100)))
                .build();

        var metric = new ToolResultUtilizationMetric(embeddingModel, 0.6);
        EvalScore score = metric.evaluate(tc);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreLowWhenOutputIgnoresToolResults() {
        when(embeddingModel.embed("I don't know"))
                .thenReturn(List.of(1.0, 0.0, 0.0));
        when(embeddingModel.embed("sunny, 25C"))
                .thenReturn(List.of(0.0, 1.0, 0.0));

        var tc = AgentTestCase.builder()
                .input("What's the weather?")
                .actualOutput("I don't know")
                .toolCalls(List.of(
                        new ToolCall("getWeather", Map.of(), "sunny, 25C", 100)))
                .build();

        var metric = new ToolResultUtilizationMetric(embeddingModel, 0.6);
        EvalScore score = metric.evaluate(tc);

        assertThat(score.value()).isCloseTo(0.0, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldReturnPerfectScoreWhenNoToolResults() {
        var tc = AgentTestCase.builder()
                .input("Hello")
                .actualOutput("Hi there")
                .toolCalls(List.of(ToolCall.of("noResult")))
                .build();

        var metric = new ToolResultUtilizationMetric(embeddingModel);
        EvalScore score = metric.evaluate(tc);

        assertThat(score.value()).isEqualTo(1.0);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ToolResultUtilizationMetric(embeddingModel);
        assertThat(metric.name()).isEqualTo("ToolResultUtilization");
    }

    @Test
    void shouldRejectNullEmbeddingModel() {
        assertThatThrownBy(() -> new ToolResultUtilizationMetric(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectMissingActualOutput() {
        var tc = AgentTestCase.builder().input("q").build();
        var metric = new ToolResultUtilizationMetric(embeddingModel);

        assertThatThrownBy(() -> metric.evaluate(tc))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
