package com.agenteval.reporting;

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

class JunitXmlReporterTest {

    @Test
    void shouldGenerateValidXml(@TempDir Path tempDir) throws Exception {
        var tc = AgentTestCase.builder()
                .input("How do I get a refund?")
                .actualOutput("You can request a refund.")
                .build();
        var score = new EvalScore(0.85, 0.7, true, "Good answer", "Relevancy");
        var caseResult = new CaseResult(tc, Map.of("Relevancy", score), true);
        var result = EvalResult.of(List.of(caseResult), 500);

        Path outFile = tempDir.resolve("report.xml");
        var reporter = new JunitXmlReporter(outFile);
        reporter.report(result);

        String xml = Files.readString(outFile);
        assertThat(xml).contains("<testsuite");
        assertThat(xml).contains("name=\"AgentEval\"");
        assertThat(xml).contains("tests=\"1\"");
        assertThat(xml).contains("failures=\"0\"");
        assertThat(xml).contains("<testcase");
        assertThat(xml).doesNotContain("<failure");
    }

    @Test
    void shouldIncludeFailureElements(@TempDir Path tempDir) throws Exception {
        var tc = AgentTestCase.builder()
                .input("Where is my order?")
                .actualOutput("I don't know.")
                .build();
        var score = new EvalScore(0.3, 0.7, false, "Poor relevancy", "Relevancy");
        var caseResult = new CaseResult(tc, Map.of("Relevancy", score), false);
        var result = EvalResult.of(List.of(caseResult), 200);

        Path outFile = tempDir.resolve("report.xml");
        var reporter = new JunitXmlReporter(outFile);
        reporter.report(result);

        String xml = Files.readString(outFile);
        assertThat(xml).contains("failures=\"1\"");
        assertThat(xml).contains("<failure");
        assertThat(xml).contains("type=\"MetricFailure\"");
        assertThat(xml).contains("Poor relevancy");
    }

    @Test
    void shouldHandleMultipleMetricsPerCase(@TempDir Path tempDir) throws Exception {
        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();
        var score1 = new EvalScore(0.9, 0.7, true, "good", "Metric1");
        var score2 = new EvalScore(0.4, 0.6, false, "bad", "Metric2");
        var caseResult = new CaseResult(tc,
                Map.of("Metric1", score1, "Metric2", score2), false);
        var result = EvalResult.of(List.of(caseResult), 100);

        Path outFile = tempDir.resolve("report.xml");
        var reporter = new JunitXmlReporter(outFile);
        reporter.report(result);

        String xml = Files.readString(outFile);
        assertThat(xml).contains("tests=\"2\"");
        assertThat(xml).contains("failures=\"1\"");
    }

    @Test
    void shouldHandleEmptyResults(@TempDir Path tempDir) throws Exception {
        var result = EvalResult.of(List.of(), 0);
        Path outFile = tempDir.resolve("report.xml");
        new JunitXmlReporter(outFile).report(result);

        String xml = Files.readString(outFile);
        assertThat(xml).contains("tests=\"0\"");
        assertThat(xml).contains("failures=\"0\"");
    }
}
