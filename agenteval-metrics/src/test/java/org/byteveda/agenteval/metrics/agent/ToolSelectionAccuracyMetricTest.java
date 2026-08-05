package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ToolSelectionAccuracyMetricTest {

    private final ToolSelectionAccuracyMetric metric = new ToolSelectionAccuracyMetric();

    @Test
    void shouldReturnPerfectScoreForExactMatch() {
        var testCase = AgentTestCase.builder()
                .input("Search and summarize")
                .toolCalls(List.of(ToolCall.of("search"), ToolCall.of("summarize")))
                .expectedToolCalls(List.of(ToolCall.of("search"), ToolCall.of("summarize")))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldReturnZeroForNoOverlap() {
        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of(ToolCall.of("toolA")))
                .expectedToolCalls(List.of(ToolCall.of("toolB")))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.0, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldCalculateF1ForPartialMatch() {
        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of(ToolCall.of("search"), ToolCall.of("calculate")))
                .expectedToolCalls(List.of(ToolCall.of("search"), ToolCall.of("summarize")))
                .build();

        EvalScore score = metric.evaluate(testCase);

        // precision = 1/2 = 0.5, recall = 1/2 = 0.5, F1 = 0.5
        assertThat(score.value()).isCloseTo(0.5, within(0.001));
    }

    @Test
    void shouldHandlePerfectPrecisionPartialRecall() {
        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of(ToolCall.of("search")))
                .expectedToolCalls(List.of(ToolCall.of("search"), ToolCall.of("summarize")))
                .build();

        EvalScore score = metric.evaluate(testCase);

        // precision = 1/1 = 1.0, recall = 1/2 = 0.5, F1 = 2*1*0.5/(1+0.5) = 0.667
        assertThat(score.value()).isCloseTo(0.667, within(0.01));
    }

    @Test
    void shouldReturnPerfectForBothEmpty() {
        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of())
                .expectedToolCalls(List.of())
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void shouldReturnZeroWhenExpectedIsEmpty() {
        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of(ToolCall.of("search")))
                .expectedToolCalls(List.of())
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void shouldReturnZeroWhenActualIsEmpty() {
        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of())
                .expectedToolCalls(List.of(ToolCall.of("search")))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void shouldUseLCSWhenOrderMatters() {
        var orderedMetric = new ToolSelectionAccuracyMetric(0.8, true);

        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of(
                        ToolCall.of("search"),
                        ToolCall.of("fetch"),
                        ToolCall.of("summarize")))
                .expectedToolCalls(List.of(
                        ToolCall.of("search"),
                        ToolCall.of("summarize")))
                .build();

        EvalScore score = orderedMetric.evaluate(testCase);

        // LCS = [search, summarize] = 2, max(3,2) = 3, score = 2/3 = 0.667
        assertThat(score.value()).isCloseTo(0.667, within(0.01));
    }

    @Test
    void shouldReturnPerfectLCSForSameOrder() {
        var orderedMetric = new ToolSelectionAccuracyMetric(0.8, true);

        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of(ToolCall.of("a"), ToolCall.of("b"), ToolCall.of("c")))
                .expectedToolCalls(List.of(ToolCall.of("a"), ToolCall.of("b"), ToolCall.of("c")))
                .build();

        EvalScore score = orderedMetric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void shouldPenalizeReversedOrder() {
        var orderedMetric = new ToolSelectionAccuracyMetric(0.8, true);

        var testCase = AgentTestCase.builder()
                .input("test")
                .toolCalls(List.of(ToolCall.of("c"), ToolCall.of("b"), ToolCall.of("a")))
                .expectedToolCalls(List.of(ToolCall.of("a"), ToolCall.of("b"), ToolCall.of("c")))
                .build();

        EvalScore score = orderedMetric.evaluate(testCase);

        // LCS of [c,b,a] vs [a,b,c] = 1 (just "b"), score = 1/3
        assertThat(score.value()).isCloseTo(0.333, within(0.01));
    }

    @Test
    void shouldReturnCorrectName() {
        assertThat(metric.name()).isEqualTo("ToolSelectionAccuracy");
    }

    @Test
    void shouldRejectInvalidThreshold() {
        assertThatThrownBy(() -> new ToolSelectionAccuracyMetric(1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullTestCase() {
        assertThatThrownBy(() -> metric.evaluate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void lcsOfEmptyLists() {
        assertThat(ToolSelectionAccuracyMetric.longestCommonSubsequence(
                List.of(), List.of())).isZero();
    }
}
