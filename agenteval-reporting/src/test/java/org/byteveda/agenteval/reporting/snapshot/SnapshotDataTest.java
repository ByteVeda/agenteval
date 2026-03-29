package org.byteveda.agenteval.reporting.snapshot;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.reporting.regression.RegressionComparison;
import org.byteveda.agenteval.reporting.regression.RegressionReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SnapshotDataTest {

    @Test
    void fromEvalResultCapturesAllFields() {
        EvalResult result = makeResult("What is Java?", "Relevancy", 0.9, 0.7, true);
        SnapshotData snapshot = SnapshotData.from("test", result);

        assertThat(snapshot.snapshotName()).isEqualTo("test");
        assertThat(snapshot.averageScore()).isCloseTo(0.9, within(0.001));
        assertThat(snapshot.passRate()).isCloseTo(1.0, within(0.001));
        assertThat(snapshot.totalCases()).isEqualTo(1);
        assertThat(snapshot.createdAt()).isNotNull();
        assertThat(snapshot.metricAverages()).containsEntry("Relevancy", 0.9);
    }

    @Test
    void fromEvalResultPreservesCaseData() {
        EvalResult result = makeResult("What is Java?", "Relevancy", 0.9, 0.7, true);
        SnapshotData snapshot = SnapshotData.from("test", result);

        assertThat(snapshot.caseResults()).hasSize(1);
        SnapshotCaseData caseData = snapshot.caseResults().get(0);
        assertThat(caseData.input()).isEqualTo("What is Java?");
        assertThat(caseData.passed()).isTrue();
        assertThat(caseData.scores()).containsKey("Relevancy");
        assertThat(caseData.scores().get("Relevancy").value()).isCloseTo(0.9, within(0.001));
    }

    @Test
    void toEvalResultRoundTrip() {
        EvalResult original = makeResult("What is Java?", "Relevancy", 0.85, 0.7, true);
        SnapshotData snapshot = SnapshotData.from("round-trip", original);
        EvalResult reconstructed = snapshot.toEvalResult();

        assertThat(reconstructed.averageScore())
                .isCloseTo(original.averageScore(), within(0.001));
        assertThat(reconstructed.passRate())
                .isCloseTo(original.passRate(), within(0.001));
        assertThat(reconstructed.caseResults()).hasSize(1);
        assertThat(reconstructed.caseResults().get(0).testCase().getInput())
                .isEqualTo("What is Java?");
        assertThat(reconstructed.caseResults().get(0).scores().get("Relevancy").value())
                .isCloseTo(0.85, within(0.001));
    }

    @Test
    void toEvalResultWorksWithRegressionComparison() {
        EvalResult baseline = makeResult("Q1", "M1", 0.9, 0.7, true);
        SnapshotData snapshot = SnapshotData.from("baseline", baseline);

        EvalResult current = makeResult("Q1", "M1", 0.5, 0.7, false);
        EvalResult reconstructedBaseline = snapshot.toEvalResult();

        RegressionReport report = RegressionComparison.compare(reconstructedBaseline, current);
        assertThat(report.hasRegressions()).isTrue();
        assertThat(report.newFailures()).isEqualTo(1);
    }

    @Test
    void multiCaseRoundTrip() {
        AgentTestCase tc1 = AgentTestCase.builder()
                .input("Q1").actualOutput("A1").build();
        AgentTestCase tc2 = AgentTestCase.builder()
                .input("Q2").actualOutput("A2").build();

        EvalScore s1 = new EvalScore(0.9, 0.7, true, "good", "M1");
        EvalScore s2 = new EvalScore(0.4, 0.7, false, "bad", "M1");

        EvalResult result = EvalResult.of(List.of(
                new CaseResult(tc1, Map.of("M1", s1), true),
                new CaseResult(tc2, Map.of("M1", s2), false)
        ), 150L);

        SnapshotData snapshot = SnapshotData.from("multi", result);
        EvalResult reconstructed = snapshot.toEvalResult();

        assertThat(reconstructed.caseResults()).hasSize(2);
        assertThat(reconstructed.failedCases()).hasSize(1);
        assertThat(reconstructed.caseResults().get(0).passed()).isTrue();
        assertThat(reconstructed.caseResults().get(1).passed()).isFalse();
    }

    private static EvalResult makeResult(String input, String metric,
                                          double score, double threshold, boolean passed) {
        AgentTestCase tc = AgentTestCase.builder()
                .input(input).actualOutput("answer").build();
        EvalScore evalScore = new EvalScore(score, threshold, passed, "test reason", metric);
        CaseResult cr = new CaseResult(tc, Map.of(metric, evalScore), passed);
        return EvalResult.of(List.of(cr), 100L);
    }
}
