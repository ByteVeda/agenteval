package com.agenteval.reporting;

import com.agenteval.core.eval.CaseResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JsonReporterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldWriteJsonReport(@TempDir Path tmpDir) throws Exception {
        Path outputFile = tmpDir.resolve("report.json");

        var testCase = AgentTestCase.builder()
                .input("What is Java?")
                .actualOutput("A programming language")
                .build();
        var score = EvalScore.of(0.9, 0.7, "Good answer");
        var caseResult = new CaseResult(testCase, Map.of("Relevancy", score), true);
        var result = EvalResult.of(List.of(caseResult), 150);

        new JsonReporter(outputFile).report(result);

        assertThat(outputFile).exists();
        JsonNode root = MAPPER.readTree(Files.readString(outputFile));

        assertThat(root.path("totalCases").asInt()).isEqualTo(1);
        assertThat(root.path("failedCases").asInt()).isEqualTo(0);
        assertThat(root.path("durationMs").asLong()).isEqualTo(150);
        assertThat(root.path("passRate").asDouble()).isCloseTo(1.0, within(0.001));
        assertThat(root.path("averageScore").asDouble()).isCloseTo(0.9, within(0.001));

        JsonNode metricAvg = root.path("metricAverages");
        assertThat(metricAvg.path("Relevancy").asDouble()).isCloseTo(0.9, within(0.001));

        JsonNode cases = root.path("caseResults");
        assertThat(cases).hasSize(1);
        assertThat(cases.get(0).path("input").asText()).isEqualTo("What is Java?");
        assertThat(cases.get(0).path("passed").asBoolean()).isTrue();

        JsonNode scores = cases.get(0).path("scores").path("Relevancy");
        assertThat(scores.path("value").asDouble()).isCloseTo(0.9, within(0.001));
        assertThat(scores.path("reason").asText()).isEqualTo("Good answer");
    }

    @Test
    void shouldReportFailedCases(@TempDir Path tmpDir) throws Exception {
        Path outputFile = tmpDir.resolve("report.json");

        var testCase = AgentTestCase.builder()
                .input("Question").actualOutput("Bad answer").build();
        var score = EvalScore.of(0.3, 0.7, "Irrelevant");
        var caseResult = new CaseResult(testCase, Map.of("Relevancy", score), false);
        var result = EvalResult.of(List.of(caseResult), 100);

        new JsonReporter(outputFile).report(result);

        JsonNode root = MAPPER.readTree(Files.readString(outputFile));
        assertThat(root.path("failedCases").asInt()).isEqualTo(1);
        assertThat(root.path("passRate").asDouble()).isCloseTo(0.0, within(0.001));
    }
}
