package org.byteveda.agenteval.reporting;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleReporterTest {

    @Test
    void shouldReportPassingResults() {
        var result = buildResult(0.85, 0.7, true);
        String output = capture(result, false);

        assertThat(output).contains("=== AgentEval Results ===");
        assertThat(output).contains("Cases: 1");
        assertThat(output).contains("Passed: 1");
        assertThat(output).contains("Failed: 0");
        assertThat(output).contains("Pass Rate: 100.0%");
        assertThat(output).contains("Average Score:");
        assertThat(output).doesNotContain("--- Failed Cases ---");
    }

    @Test
    void shouldReportFailingResults() {
        var result = buildResult(0.45, 0.7, false);
        String output = capture(result, false);

        assertThat(output).contains("Failed: 1");
        assertThat(output).contains("--- Failed Cases ---");
        assertThat(output).contains("test-metric");
        assertThat(output).contains("0.45");
    }

    @Test
    void shouldReportPerMetricAverages() {
        var result = buildResult(0.85, 0.7, true);
        String output = capture(result, false);

        assertThat(output).contains("--- Per-Metric Averages ---");
        assertThat(output).contains("test-metric");
    }

    @Test
    void shouldRespectAnsiColorsFalse() {
        var result = buildResult(0.85, 0.7, true);
        String output = capture(result, false);

        assertThat(output).doesNotContain("\u001B[");
        assertThat(output).contains("PASS");
    }

    @Test
    void shouldUseAnsiColorsWhenEnabled() {
        var result = buildResult(0.85, 0.7, true);
        String output = capture(result, true);

        assertThat(output).contains("\u001B[");
    }

    @Test
    void shouldHandleEmptyResults() {
        var result = EvalResult.of(List.of(), 0);
        String output = capture(result, false);

        assertThat(output).contains("Cases: 0");
        assertThat(output).contains("Pass Rate: 0.0%");
    }

    private String capture(EvalResult result, boolean ansi) {
        var baos = new ByteArrayOutputStream();
        var reporter = new ConsoleReporter(new PrintStream(baos), ansi);
        reporter.report(result);
        return baos.toString(StandardCharsets.UTF_8);
    }

    private EvalResult buildResult(double score, double threshold, boolean passed) {
        var tc = AgentTestCase.builder().input("test input").actualOutput("test output").build();
        var evalScore = new EvalScore(score, threshold, passed, "test reason", "test-metric");
        var caseResult = new CaseResult(tc, Map.of("test-metric", evalScore), passed);
        return EvalResult.of(List.of(caseResult), 1234);
    }
}
