package org.byteveda.agenteval.reporting.benchmark;

import org.byteveda.agenteval.core.benchmark.BenchmarkResult;
import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkReporterTest {

    @Test
    void reportsTableWithBestWorstLabels() {
        BenchmarkResult result = makeBenchmarkResult(0.9, 0.4);
        String output = capture(result, false);

        assertThat(output).contains("=== Benchmark Results ===");
        assertThat(output).contains("high");
        assertThat(output).contains("low");
        assertThat(output).contains("[BEST]");
        assertThat(output).contains("[WORST]");
    }

    @Test
    void perMetricBreakdownShown() {
        BenchmarkResult result = makeBenchmarkResult(0.9, 0.4);
        String output = capture(result, false);

        assertThat(output).contains("--- Per-Metric Breakdown ---");
        assertThat(output).contains("M1");
    }

    @Test
    void singleVariantNoLabels() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("only", makeResult(0.7));

        BenchmarkResult br = new BenchmarkResult(results, 100L);
        String output = capture(br, false);

        assertThat(output).contains("only");
        assertThat(output).doesNotContain("[BEST]");
        assertThat(output).doesNotContain("[WORST]");
    }

    @Test
    void ansiColorsToggle() {
        BenchmarkResult result = makeBenchmarkResult(0.9, 0.4);

        String withAnsi = capture(result, true);
        String withoutAnsi = capture(result, false);

        // ANSI version should have escape codes
        assertThat(withAnsi).contains("\u001B[");
        assertThat(withoutAnsi).doesNotContain("\u001B[");
    }

    @Test
    void showsVariantCountAndDuration() {
        BenchmarkResult result = makeBenchmarkResult(0.9, 0.4);
        String output = capture(result, false);

        assertThat(output).contains("Variants: 2");
        assertThat(output).contains("Duration:");
    }

    private String capture(BenchmarkResult result, boolean ansi) {
        var baos = new ByteArrayOutputStream();
        var ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
        new BenchmarkReporter(ps, ansi).report(result);
        return baos.toString(StandardCharsets.UTF_8);
    }

    private static BenchmarkResult makeBenchmarkResult(double highScore, double lowScore) {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("high", makeResult(highScore));
        results.put("low", makeResult(lowScore));
        return new BenchmarkResult(results, 500L);
    }

    private static EvalResult makeResult(double score) {
        AgentTestCase tc = AgentTestCase.builder()
                .input("q").actualOutput("a").build();
        EvalScore s = new EvalScore(score, 0.7, score >= 0.7, "test", "M1");
        CaseResult cr = new CaseResult(tc, Map.of("M1", s), score >= 0.7);
        return EvalResult.of(List.of(cr), 100L);
    }
}
