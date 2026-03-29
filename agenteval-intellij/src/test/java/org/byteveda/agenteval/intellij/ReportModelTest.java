package org.byteveda.agenteval.intellij;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReportModelTest {

    @Test
    void metricAveragesAccessible() throws IOException {
        String json = """
                {
                  "averageScore": 0.85,
                  "passRate": 1.0,
                  "totalCases": 1,
                  "failedCases": 0,
                  "durationMs": 100,
                  "metricAverages": {"M1": 0.9, "M2": 0.8},
                  "caseResults": []
                }
                """;

        ReportModel report = ReportParser.parse(json);

        assertThat(report.getMetricAverages()).hasSize(2);
        assertThat(report.getMetricAverages().get("M1")).isCloseTo(0.9, within(0.001));
        assertThat(report.getMetricAverages().get("M2")).isCloseTo(0.8, within(0.001));
    }

    @Test
    void overallPassWhenNoFailures() throws IOException {
        String json = """
                {
                  "averageScore": 0.9,
                  "passRate": 1.0,
                  "totalCases": 2,
                  "failedCases": 0,
                  "durationMs": 200,
                  "metricAverages": {},
                  "caseResults": []
                }
                """;

        ReportModel report = ReportParser.parse(json);
        assertThat(report.isOverallPass()).isTrue();
    }

    @Test
    void overallFailWhenHasFailures() throws IOException {
        String json = """
                {
                  "averageScore": 0.5,
                  "passRate": 0.5,
                  "totalCases": 2,
                  "failedCases": 1,
                  "durationMs": 200,
                  "metricAverages": {},
                  "caseResults": []
                }
                """;

        ReportModel report = ReportParser.parse(json);
        assertThat(report.isOverallPass()).isFalse();
    }

    @Test
    void scoreModelFieldsAccessible() throws IOException {
        String json = """
                {
                  "averageScore": 0.9,
                  "passRate": 1.0,
                  "totalCases": 1,
                  "failedCases": 0,
                  "durationMs": 50,
                  "metricAverages": {"M1": 0.9},
                  "caseResults": [{
                    "input": "Q", "passed": true, "averageScore": 0.9,
                    "scores": {"M1": {"value": 0.9, "threshold": 0.7, "passed": true, "reason": "good"}}
                  }]
                }
                """;

        ReportModel report = ReportParser.parse(json);
        var score = report.getCaseResults().getFirst().getScores().get("M1");

        assertThat(score.getValue()).isCloseTo(0.9, within(0.001));
        assertThat(score.getThreshold()).isCloseTo(0.7, within(0.001));
        assertThat(score.isPassed()).isTrue();
        assertThat(score.getReason()).isEqualTo("good");
    }

    @Test
    void unknownFieldsIgnored() throws IOException {
        String json = """
                {
                  "averageScore": 0.9,
                  "passRate": 1.0,
                  "totalCases": 1,
                  "failedCases": 0,
                  "durationMs": 50,
                  "metricAverages": {},
                  "caseResults": [],
                  "extraField": "ignored"
                }
                """;

        ReportModel report = ReportParser.parse(json);
        assertThat(report.getAverageScore()).isCloseTo(0.9, within(0.001));
    }
}
