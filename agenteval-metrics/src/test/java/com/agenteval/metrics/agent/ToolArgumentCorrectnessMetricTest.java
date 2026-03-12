package com.agenteval.metrics.agent;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import com.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ToolArgumentCorrectnessMetricTest {

    @Test
    void shouldScorePerfectMatch() {
        var metric = new ToolArgumentCorrectnessMetric(0.8);
        var testCase = AgentTestCase.builder()
                .input("Search for Java tutorials")
                .toolCalls(List.of(
                        ToolCall.of("search", Map.of("query", "Java tutorials"))))
                .expectedToolCalls(List.of(
                        ToolCall.of("search", Map.of("query", "Java tutorials"))))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScorePartialMatch() {
        var metric = new ToolArgumentCorrectnessMetric(0.8);
        var testCase = AgentTestCase.builder()
                .input("Search query")
                .toolCalls(List.of(
                        ToolCall.of("search", Map.of("query", "wrong", "limit", 10))))
                .expectedToolCalls(List.of(
                        ToolCall.of("search", Map.of("query", "correct", "limit", 10))))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.5, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldCountExtraArgsInStrictMode() {
        var metric = new ToolArgumentCorrectnessMetric(0.8, true);
        var testCase = AgentTestCase.builder()
                .input("Search query")
                .toolCalls(List.of(
                        ToolCall.of("search", Map.of("query", "test", "extra", "val"))))
                .expectedToolCalls(List.of(
                        ToolCall.of("search", Map.of("query", "test"))))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.5, within(0.001));
    }

    @Test
    void shouldNotCountExtraArgsInNonStrictMode() {
        var metric = new ToolArgumentCorrectnessMetric(0.8, false);
        var testCase = AgentTestCase.builder()
                .input("Search query")
                .toolCalls(List.of(
                        ToolCall.of("search", Map.of("query", "test", "extra", "val"))))
                .expectedToolCalls(List.of(
                        ToolCall.of("search", Map.of("query", "test"))))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void shouldHandleBothEmpty() {
        var metric = new ToolArgumentCorrectnessMetric();
        var testCase = AgentTestCase.builder()
                .input("query")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void shouldHandleMissingActualCalls() {
        var metric = new ToolArgumentCorrectnessMetric();
        var testCase = AgentTestCase.builder()
                .input("query")
                .expectedToolCalls(List.of(ToolCall.of("search", Map.of("q", "test"))))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ToolArgumentCorrectnessMetric();
        assertThat(metric.name()).isEqualTo("ToolArgumentCorrectness");
    }
}
