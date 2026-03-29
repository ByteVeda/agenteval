package org.byteveda.agenteval.reporting.regression;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RegressionComparisonTest {

    @Test
    void shouldDetectImprovement() {
        var baseline = makeResult("q1", "M1", 0.5, 0.7, false);
        var current = makeResult("q1", "M1", 0.9, 0.7, true);

        RegressionReport report = RegressionComparison.compare(baseline, current);

        assertThat(report.overallDelta()).isGreaterThan(0);
        assertThat(report.newPasses()).isEqualTo(1);
        assertThat(report.newFailures()).isEqualTo(0);
        assertThat(report.hasRegressions()).isFalse();
    }

    @Test
    void shouldDetectRegression() {
        var baseline = makeResult("q1", "M1", 0.9, 0.7, true);
        var current = makeResult("q1", "M1", 0.4, 0.7, false);

        RegressionReport report = RegressionComparison.compare(baseline, current);

        assertThat(report.overallDelta()).isLessThan(0);
        assertThat(report.newFailures()).isEqualTo(1);
        assertThat(report.newPasses()).isEqualTo(0);
        assertThat(report.hasRegressions()).isTrue();
    }

    @Test
    void shouldHandleNoOverlap() {
        var baseline = makeResult("q1", "M1", 0.9, 0.7, true);
        var current = makeResult("q2", "M1", 0.8, 0.7, true);

        RegressionReport report = RegressionComparison.compare(baseline, current);

        assertThat(report.caseChanges()).isEmpty();
        assertThat(report.newFailures()).isEqualTo(0);
    }

    @Test
    void shouldComputePerMetricDeltas() {
        var baseline = makeResult("q1", "M1", 0.6, 0.5, true);
        var current = makeResult("q1", "M1", 0.8, 0.5, true);

        RegressionReport report = RegressionComparison.compare(baseline, current);

        assertThat(report.metricDeltas()).containsKey("M1");
        MetricDelta delta = report.metricDeltas().get("M1");
        assertThat(delta.baselineScore()).isCloseTo(0.6, within(0.001));
        assertThat(delta.currentScore()).isCloseTo(0.8, within(0.001));
        assertThat(delta.improved()).isTrue();
    }

    @Test
    void shouldHandleEmptyResults() {
        var baseline = EvalResult.of(List.of(), 0);
        var current = EvalResult.of(List.of(), 0);

        RegressionReport report = RegressionComparison.compare(baseline, current);

        assertThat(report.caseChanges()).isEmpty();
        assertThat(report.overallDelta()).isEqualTo(0.0);
    }

    private static EvalResult makeResult(String input, String metric,
                                          double score, double threshold, boolean passed) {
        var tc = AgentTestCase.builder().input(input).actualOutput("out").build();
        var evalScore = new EvalScore(score, threshold, passed,
                passed ? "ok" : "fail", metric);
        var caseResult = new CaseResult(tc, Map.of(metric, evalScore), passed);
        return EvalResult.of(List.of(caseResult), 100);
    }
}
