package com.agenteval.github;

import com.agenteval.core.eval.CaseResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReporterTest {

    @Test
    void shouldGenerateMarkdownWithSummaryTable(@TempDir Path tempDir) throws Exception {
        Path outPath = tempDir.resolve("report.md");
        var config = MarkdownConfig.builder()
                .outputPath(outPath)
                .includeMetricBreakdown(true)
                .includeFailedDetails(true)
                .build();

        var reporter = new MarkdownReporter(config);
        reporter.report(sampleResult());

        assertThat(outPath).exists();
        String content = Files.readString(outPath);
        assertThat(content).contains("## AgentEval Results");
        assertThat(content).contains("| Status |");
        assertThat(content).contains("Cases");
        assertThat(content).contains("Pass Rate");
    }

    @Test
    void shouldIncludeMetricBreakdown() {
        var config = MarkdownConfig.builder()
                .outputPath(Path.of("test.md"))
                .includeMetricBreakdown(true)
                .build();
        var reporter = new MarkdownReporter(config);

        String markdown = reporter.render(sampleResult());

        assertThat(markdown).contains("### Per-Metric Averages");
        assertThat(markdown).contains("Relevancy");
    }

    @Test
    void shouldExcludeMetricBreakdownWhenDisabled() {
        var config = MarkdownConfig.builder()
                .outputPath(Path.of("test.md"))
                .includeMetricBreakdown(false)
                .build();
        var reporter = new MarkdownReporter(config);

        String markdown = reporter.render(sampleResult());

        assertThat(markdown).doesNotContain("### Per-Metric Averages");
    }

    @Test
    void shouldIncludeFailedDetailsInCollapsible() {
        var config = MarkdownConfig.builder()
                .outputPath(Path.of("test.md"))
                .includeFailedDetails(true)
                .build();
        var reporter = new MarkdownReporter(config);

        String markdown = reporter.render(resultWithFailures());

        assertThat(markdown).contains("<details>");
        assertThat(markdown).contains("Failed Cases");
        assertThat(markdown).contains("</details>");
    }

    @Test
    void shouldExcludeFailedDetailsWhenDisabled() {
        var config = MarkdownConfig.builder()
                .outputPath(Path.of("test.md"))
                .includeFailedDetails(false)
                .build();
        var reporter = new MarkdownReporter(config);

        String markdown = reporter.render(resultWithFailures());

        assertThat(markdown).doesNotContain("<details>");
    }

    @Test
    void shouldShowCheckmarkForAllPassing() {
        var config = MarkdownConfig.builder()
                .outputPath(Path.of("test.md"))
                .build();
        var reporter = new MarkdownReporter(config);

        String markdown = reporter.render(sampleResult());

        assertThat(markdown).contains("✅");
    }

    @Test
    void shouldShowCrossForFailures() {
        var config = MarkdownConfig.builder()
                .outputPath(Path.of("test.md"))
                .build();
        var reporter = new MarkdownReporter(config);

        String markdown = reporter.render(resultWithFailures());

        assertThat(markdown).contains("❌");
    }

    private static EvalResult sampleResult() {
        var tc = AgentTestCase.builder().input("What is AI?").build();
        var score = new EvalScore(0.9, 0.7, true, "Good answer", "Relevancy");
        var caseResult = new CaseResult(tc, Map.of("Relevancy", score), true);
        return EvalResult.of(List.of(caseResult), 150);
    }

    private static EvalResult resultWithFailures() {
        var tc1 = AgentTestCase.builder().input("What is AI?").build();
        var score1 = new EvalScore(0.9, 0.7, true, "Good answer", "Relevancy");

        var tc2 = AgentTestCase.builder().input("Explain quantum computing").build();
        var score2 = new EvalScore(0.3, 0.7, false, "Off topic", "Relevancy");

        return EvalResult.of(List.of(
                new CaseResult(tc1, Map.of("Relevancy", score1), true),
                new CaseResult(tc2, Map.of("Relevancy", score2), false)
        ), 200);
    }
}
