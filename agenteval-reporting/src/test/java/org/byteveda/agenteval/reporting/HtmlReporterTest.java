package org.byteveda.agenteval.reporting;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlReporterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGenerateHtmlReport() throws IOException {
        Path output = tempDir.resolve("report.html");
        var config = HtmlReportConfig.builder()
                .outputPath(output)
                .title("Test Report")
                .build();

        var tc = AgentTestCase.builder().input("q1").actualOutput("a1").build();
        var scores = Map.of("Metric1",
                new EvalScore(0.9, 0.7, true, "good", "Metric1"));
        var caseResult = new CaseResult(tc, scores, true);
        var evalResult = EvalResult.of(List.of(caseResult), 100);

        new HtmlReporter(config).report(evalResult);

        String html = Files.readString(output);
        assertThat(html).contains("Test Report");
        assertThat(html).contains("AgentEval");
        assertThat(html).contains("0.9");
    }

    @Test
    void shouldBuildValidJsonData() {
        var config = HtmlReportConfig.builder()
                .outputPath(tempDir.resolve("test.html"))
                .title("JSON Test")
                .includeDetails(true)
                .build();

        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();
        var scores = Map.of("M1",
                new EvalScore(0.8, 0.5, true, "ok", "M1"));
        var caseResult = new CaseResult(tc, scores, true);
        var evalResult = EvalResult.of(List.of(caseResult), 50);

        var reporter = new HtmlReporter(config);
        String json = reporter.buildJsonData(evalResult);

        assertThat(json).contains("\"title\"");
        assertThat(json).contains("\"averageScore\"");
        assertThat(json).contains("\"caseResults\"");
    }

    @Test
    void shouldOmitDetailsWhenConfigured() {
        var config = HtmlReportConfig.builder()
                .outputPath(tempDir.resolve("test.html"))
                .includeDetails(false)
                .build();

        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();
        var caseResult = new CaseResult(tc, Map.of(), true);
        var evalResult = EvalResult.of(List.of(caseResult), 10);

        var reporter = new HtmlReporter(config);
        String json = reporter.buildJsonData(evalResult);

        assertThat(json).doesNotContain("\"caseResults\"");
    }
}
