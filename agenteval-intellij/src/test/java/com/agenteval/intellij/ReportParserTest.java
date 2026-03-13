package com.agenteval.intellij;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ReportParserTest {

    @Test
    void parseSingleCaseReport() throws IOException {
        String json = """
                {
                  "averageScore": 0.9,
                  "passRate": 1.0,
                  "totalCases": 1,
                  "failedCases": 0,
                  "durationMs": 150,
                  "metricAverages": {"Relevancy": 0.9},
                  "caseResults": [{
                    "input": "What is Java?",
                    "passed": true,
                    "averageScore": 0.9,
                    "scores": {
                      "Relevancy": {
                        "value": 0.9,
                        "threshold": 0.7,
                        "passed": true,
                        "reason": "Good answer"
                      }
                    }
                  }]
                }
                """;

        ReportModel report = ReportParser.parse(json);

        assertThat(report.getAverageScore()).isCloseTo(0.9, within(0.001));
        assertThat(report.getPassRate()).isCloseTo(1.0, within(0.001));
        assertThat(report.getTotalCases()).isEqualTo(1);
        assertThat(report.getFailedCases()).isEqualTo(0);
        assertThat(report.getDurationMs()).isEqualTo(150L);
        assertThat(report.getMetricAverages()).containsEntry("Relevancy", 0.9);
        assertThat(report.getCaseResults()).hasSize(1);

        var cr = report.getCaseResults().getFirst();
        assertThat(cr.getInput()).isEqualTo("What is Java?");
        assertThat(cr.isPassed()).isTrue();
        assertThat(cr.getScores().get("Relevancy").getValue()).isCloseTo(0.9, within(0.001));
        assertThat(cr.getScores().get("Relevancy").getReason()).isEqualTo("Good answer");
    }

    @Test
    void parseMultiCaseReport() throws IOException {
        String json = """
                {
                  "averageScore": 0.7,
                  "passRate": 0.5,
                  "totalCases": 2,
                  "failedCases": 1,
                  "durationMs": 300,
                  "metricAverages": {"M1": 0.7},
                  "caseResults": [
                    {"input": "Q1", "passed": true, "averageScore": 0.9,
                     "scores": {"M1": {"value": 0.9, "threshold": 0.7, "passed": true, "reason": "ok"}}},
                    {"input": "Q2", "passed": false, "averageScore": 0.5,
                     "scores": {"M1": {"value": 0.5, "threshold": 0.7, "passed": false, "reason": "bad"}}}
                  ]
                }
                """;

        ReportModel report = ReportParser.parse(json);

        assertThat(report.getTotalCases()).isEqualTo(2);
        assertThat(report.getFailedCases()).isEqualTo(1);
        assertThat(report.getCaseResults()).hasSize(2);
        assertThat(report.getCaseResults().get(0).isPassed()).isTrue();
        assertThat(report.getCaseResults().get(1).isPassed()).isFalse();
    }

    @Test
    void parseFailedCases() throws IOException {
        String json = """
                {
                  "averageScore": 0.3,
                  "passRate": 0.0,
                  "totalCases": 1,
                  "failedCases": 1,
                  "durationMs": 100,
                  "metricAverages": {"M1": 0.3},
                  "caseResults": [{
                    "input": "Q1", "passed": false, "averageScore": 0.3,
                    "scores": {"M1": {"value": 0.3, "threshold": 0.7, "passed": false, "reason": "poor"}}
                  }]
                }
                """;

        ReportModel report = ReportParser.parse(json);
        assertThat(report.isOverallPass()).isFalse();
        assertThat(report.getFailedCases()).isEqualTo(1);
    }

    @Test
    void parseMissingFieldsHandledGracefully() throws IOException {
        String json = """
                {
                  "averageScore": 0.0,
                  "passRate": 0.0,
                  "totalCases": 0,
                  "failedCases": 0,
                  "durationMs": 0,
                  "metricAverages": {},
                  "caseResults": []
                }
                """;

        ReportModel report = ReportParser.parse(json);
        assertThat(report.getCaseResults()).isEmpty();
        assertThat(report.getMetricAverages()).isEmpty();
    }

    @Test
    void malformedJsonThrowsException() {
        assertThatThrownBy(() -> ReportParser.parse("not valid json"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void parseFile(@TempDir Path tempDir) throws IOException {
        String json = """
                {
                  "averageScore": 0.85,
                  "passRate": 1.0,
                  "totalCases": 1,
                  "failedCases": 0,
                  "durationMs": 50,
                  "metricAverages": {"M1": 0.85},
                  "caseResults": [{
                    "input": "Q", "passed": true, "averageScore": 0.85,
                    "scores": {"M1": {"value": 0.85, "threshold": 0.7, "passed": true, "reason": "ok"}}
                  }]
                }
                """;
        Path file = tempDir.resolve("report.json");
        Files.writeString(file, json);

        ReportModel report = ReportParser.parseFile(file);
        assertThat(report.getAverageScore()).isCloseTo(0.85, within(0.001));
    }

    @Test
    void extractMetricPassFailAllPass() throws IOException {
        String json = """
                {
                  "averageScore": 0.9,
                  "passRate": 1.0,
                  "totalCases": 1,
                  "failedCases": 0,
                  "durationMs": 50,
                  "metricAverages": {"M1": 0.9, "M2": 0.8},
                  "caseResults": [{
                    "input": "Q", "passed": true, "averageScore": 0.85,
                    "scores": {
                      "M1": {"value": 0.9, "threshold": 0.7, "passed": true, "reason": "ok"},
                      "M2": {"value": 0.8, "threshold": 0.7, "passed": true, "reason": "ok"}
                    }
                  }]
                }
                """;

        ReportModel report = ReportParser.parse(json);
        Map<String, Boolean> status = ReportParser.extractMetricPassFail(report);

        assertThat(status).containsEntry("M1", true);
        assertThat(status).containsEntry("M2", true);
    }

    @Test
    void extractMetricPassFailMixed() throws IOException {
        String json = """
                {
                  "averageScore": 0.65,
                  "passRate": 0.5,
                  "totalCases": 2,
                  "failedCases": 1,
                  "durationMs": 100,
                  "metricAverages": {"M1": 0.65},
                  "caseResults": [
                    {"input": "Q1", "passed": true, "averageScore": 0.9,
                     "scores": {"M1": {"value": 0.9, "threshold": 0.7, "passed": true, "reason": "ok"}}},
                    {"input": "Q2", "passed": false, "averageScore": 0.4,
                     "scores": {"M1": {"value": 0.4, "threshold": 0.7, "passed": false, "reason": "bad"}}}
                  ]
                }
                """;

        ReportModel report = ReportParser.parse(json);
        Map<String, Boolean> status = ReportParser.extractMetricPassFail(report);

        // M1 failed in at least one case, so overall it's false
        assertThat(status).containsEntry("M1", false);
    }
}
