package com.agenteval.core.eval;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvalResultTest {

    private CaseResult passingCase() {
        var tc = AgentTestCase.builder().input("test").actualOutput("good").build();
        return new CaseResult(tc, Map.of(
                "Metric1", EvalScore.of(0.9, 0.7, "great"),
                "Metric2", EvalScore.of(0.8, 0.7, "good")
        ), true);
    }

    private CaseResult failingCase() {
        var tc = AgentTestCase.builder().input("test").actualOutput("bad").build();
        return new CaseResult(tc, Map.of(
                "Metric1", EvalScore.of(0.5, 0.7, "below threshold"),
                "Metric2", EvalScore.of(0.8, 0.7, "ok")
        ), false);
    }

    @Test
    void shouldCalculatePassRate() {
        var result = EvalResult.of(List.of(passingCase(), failingCase()), 100);

        assertThat(result.passRate()).isEqualTo(0.5);
    }

    @Test
    void shouldCalculateAverageScore() {
        var result = EvalResult.of(List.of(passingCase(), failingCase()), 100);

        // passingCase avg: (0.9 + 0.8) / 2 = 0.85
        // failingCase avg: (0.5 + 0.8) / 2 = 0.65
        // overall: (0.85 + 0.65) / 2 = 0.75
        assertThat(result.averageScore()).isCloseTo(0.75, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void shouldReturnFailedCases() {
        var result = EvalResult.of(List.of(passingCase(), failingCase()), 100);

        assertThat(result.failedCases()).hasSize(1);
        assertThat(result.failedCases().getFirst().passed()).isFalse();
    }

    @Test
    void shouldCalculateAverageScoresByMetric() {
        var result = EvalResult.of(List.of(passingCase(), failingCase()), 100);
        var byMetric = result.averageScoresByMetric();

        assertThat(byMetric).containsKey("Metric1");
        assertThat(byMetric).containsKey("Metric2");
        assertThat(byMetric.get("Metric1")).isCloseTo(0.7, org.assertj.core.data.Offset.offset(0.001));
        assertThat(byMetric.get("Metric2")).isCloseTo(0.8, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void emptyResultsShouldReturnZeroes() {
        var result = EvalResult.of(List.of(), 0);

        assertThat(result.passRate()).isEqualTo(0.0);
        assertThat(result.averageScore()).isEqualTo(0.0);
        assertThat(result.failedCases()).isEmpty();
    }

    @Test
    void shouldTrackDuration() {
        var result = EvalResult.of(List.of(passingCase()), 250);
        assertThat(result.durationMs()).isEqualTo(250);
    }
}
