package com.agenteval.core.benchmark;

import com.agenteval.core.eval.CaseResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BenchmarkResultTest {

    @Test
    void bestAndWorstVariant() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("high", makeResult(0.9));
        results.put("low", makeResult(0.3));

        BenchmarkResult br = new BenchmarkResult(results, 500L);

        assertThat(br.bestVariant()).isEqualTo("high");
        assertThat(br.worstVariant()).isEqualTo("low");
    }

    @Test
    void averageScoresOrderedDescending() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("low", makeResult(0.3));
        results.put("mid", makeResult(0.6));
        results.put("high", makeResult(0.9));

        BenchmarkResult br = new BenchmarkResult(results, 500L);

        var scores = br.averageScores();
        assertThat(scores).hasSize(3);
        assertThat(scores.get(0).getKey()).isEqualTo("high");
        assertThat(scores.get(1).getKey()).isEqualTo("mid");
        assertThat(scores.get(2).getKey()).isEqualTo("low");
    }

    @Test
    void scoresByMetric() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("v1", makeResult(0.8));
        results.put("v2", makeResult(0.6));

        BenchmarkResult br = new BenchmarkResult(results, 300L);
        Map<String, Map<String, Double>> byMetric = br.scoresByMetric();

        assertThat(byMetric).containsKey("TestMetric");
        assertThat(byMetric.get("TestMetric").get("v1")).isCloseTo(0.8, within(0.001));
        assertThat(byMetric.get("TestMetric").get("v2")).isCloseTo(0.6, within(0.001));
    }

    @Test
    void unknownVariantThrows() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("v1", makeResult(0.5));

        BenchmarkResult br = new BenchmarkResult(results, 100L);

        assertThatThrownBy(() -> br.resultFor("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown variant");
    }

    @Test
    void singleVariant() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("only", makeResult(0.7));

        BenchmarkResult br = new BenchmarkResult(results, 100L);

        assertThat(br.bestVariant()).isEqualTo("only");
        assertThat(br.worstVariant()).isEqualTo("only");
        assertThat(br.averageScores()).hasSize(1);
    }

    @Test
    void resultForReturnsCorrectResult() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        EvalResult expected = makeResult(0.75);
        results.put("target", expected);

        BenchmarkResult br = new BenchmarkResult(results, 100L);
        assertThat(br.resultFor("target").averageScore())
                .isCloseTo(0.75, within(0.001));
    }

    @Test
    void totalDurationTracked() {
        BenchmarkResult br = new BenchmarkResult(Map.of("v", makeResult(0.5)), 12345L);
        assertThat(br.totalDurationMs()).isEqualTo(12345L);
    }

    private static EvalResult makeResult(double score) {
        AgentTestCase tc = AgentTestCase.builder()
                .input("q").actualOutput("a").build();
        EvalScore s = new EvalScore(score, 0.7, score >= 0.7, "test", "TestMetric");
        CaseResult cr = new CaseResult(tc, Map.of("TestMetric", s), score >= 0.7);
        return EvalResult.of(List.of(cr), 100L);
    }
}
