package org.byteveda.agenteval.reporting.benchmark;

import org.byteveda.agenteval.core.benchmark.BenchmarkResult;
import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.reporting.regression.RegressionReport;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BenchmarkComparisonTest {

    @Test
    void compareTwoVariants() {
        BenchmarkResult br = makeBenchmarkResult();
        RegressionReport report = BenchmarkComparison.compareVariants(br, "baseline", "candidate");

        assertThat(report.overallBaselineScore()).isGreaterThan(0);
        assertThat(report.overallCurrentScore()).isGreaterThan(0);
    }

    @Test
    void compareAllAgainstBaseline() {
        BenchmarkResult br = makeBenchmarkResult();
        Map<String, RegressionReport> reports =
                BenchmarkComparison.compareAllAgainst(br, "baseline");

        assertThat(reports).containsKey("candidate");
        assertThat(reports).doesNotContainKey("baseline");
    }

    @Test
    void unknownBaselineThrows() {
        BenchmarkResult br = makeBenchmarkResult();

        assertThatThrownBy(() -> BenchmarkComparison.compareVariants(br, "nope", "candidate"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void regressionDetected() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("good", makeResult("q", 0.9));
        results.put("bad", makeResult("q", 0.3));

        BenchmarkResult br = new BenchmarkResult(results, 200L);
        RegressionReport report = BenchmarkComparison.compareVariants(br, "good", "bad");

        assertThat(report.hasRegressions()).isTrue();
        assertThat(report.newFailures()).isEqualTo(1);
    }

    @Test
    void improvementDetected() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("old", makeResult("q", 0.5));
        results.put("new", makeResult("q", 0.95));

        BenchmarkResult br = new BenchmarkResult(results, 200L);
        RegressionReport report = BenchmarkComparison.compareVariants(br, "old", "new");

        assertThat(report.hasRegressions()).isFalse();
        assertThat(report.newPasses()).isEqualTo(1);
    }

    private static BenchmarkResult makeBenchmarkResult() {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        results.put("baseline", makeResult("q", 0.8));
        results.put("candidate", makeResult("q", 0.85));
        return new BenchmarkResult(results, 300L);
    }

    private static EvalResult makeResult(String input, double score) {
        AgentTestCase tc = AgentTestCase.builder()
                .input(input).actualOutput("a").build();
        boolean passed = score >= 0.7;
        EvalScore s = new EvalScore(score, 0.7, passed, "test", "M1");
        CaseResult cr = new CaseResult(tc, Map.of("M1", s), passed);
        return EvalResult.of(List.of(cr), 100L);
    }
}
