package org.byteveda.agenteval.reporting.regression.rootcause;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.ToolCall;
import org.byteveda.agenteval.reporting.regression.CaseStatusChange;
import org.byteveda.agenteval.reporting.regression.MetricDelta;
import org.byteveda.agenteval.reporting.regression.RegressionReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RootCauseAnalyzerTest {

    private CaseResult makeCaseResult(String input, String output, double score,
            boolean passed) {
        AgentTestCase testCase = AgentTestCase.builder()
                .input(input)
                .actualOutput(output)
                .build();
        Map<String, EvalScore> scores = Map.of(
                "Accuracy", new EvalScore(score, 0.5, passed, "reason", "Accuracy"));
        return new CaseResult(testCase, scores, passed);
    }

    private CaseResult makeCaseResultWithTools(String input, String output, double score,
            boolean passed, List<ToolCall> tools) {
        AgentTestCase testCase = AgentTestCase.builder()
                .input(input)
                .actualOutput(output)
                .toolCalls(tools)
                .build();
        Map<String, EvalScore> scores = Map.of(
                "Accuracy", new EvalScore(score, 0.5, passed, "reason", "Accuracy"));
        return new CaseResult(testCase, scores, passed);
    }

    private CaseResult makeCaseResultWithCostAndLatency(String input, String output,
            double score, boolean passed, BigDecimal cost, long latencyMs) {
        AgentTestCase testCase = AgentTestCase.builder()
                .input(input)
                .actualOutput(output)
                .cost(cost)
                .latencyMs(latencyMs)
                .build();
        Map<String, EvalScore> scores = Map.of(
                "Accuracy", new EvalScore(score, 0.5, passed, "reason", "Accuracy"));
        return new CaseResult(testCase, scores, passed);
    }

    @Test
    void analyzeWithNoRegressionsReturnsEmptyReport() {
        CaseStatusChange noRegression = new CaseStatusChange(
                "q1", true, true,
                List.of(new MetricDelta("Accuracy", 0.8, 0.9, 0.1)));

        RegressionReport report = new RegressionReport(
                0.8, 0.9, 0.1, Map.of(), List.of(noRegression), 0, 1);

        EvalResult baseline = EvalResult.of(
                List.of(makeCaseResult("q1", "baseline output", 0.8, true)), 1000L);
        EvalResult current = EvalResult.of(
                List.of(makeCaseResult("q1", "current output", 0.9, true)), 1000L);

        RootCauseReport rootCause = RootCauseAnalyzer.analyze(report, baseline, current);

        assertEquals(0, rootCause.totalRegressedCases());
        assertTrue(rootCause.clusters().isEmpty());
        assertEquals("No regressions detected.", rootCause.summary());
    }

    @Test
    void analyzeGroupsByRegressedMetrics() {
        MetricDelta accuracyDelta = new MetricDelta("Accuracy", 0.8, 0.3, -0.5);
        CaseStatusChange reg1 = new CaseStatusChange(
                "q1", true, false, List.of(accuracyDelta));
        CaseStatusChange reg2 = new CaseStatusChange(
                "q2", true, false, List.of(accuracyDelta));

        RegressionReport report = new RegressionReport(
                0.8, 0.3, -0.5, Map.of(), List.of(reg1, reg2), 2, 0);

        EvalResult baseline = EvalResult.of(List.of(
                makeCaseResult("q1", "baseline 1", 0.8, true),
                makeCaseResult("q2", "baseline 2", 0.8, true)), 1000L);
        EvalResult current = EvalResult.of(List.of(
                makeCaseResult("q1", "current 1", 0.3, false),
                makeCaseResult("q2", "current 2", 0.3, false)), 1000L);

        RootCauseReport rootCause = RootCauseAnalyzer.analyze(report, baseline, current);

        assertEquals(2, rootCause.totalRegressedCases());
        assertEquals(1, rootCause.clusters().size());
        assertEquals("Accuracy", rootCause.clusters().get(0).clusterName());
        assertEquals(2, rootCause.clusters().get(0).cases().size());
    }

    @Test
    void analyzeDetectsOutputLengthChange() {
        MetricDelta delta = new MetricDelta("Accuracy", 0.8, 0.3, -0.5);
        CaseStatusChange reg = new CaseStatusChange(
                "q1", true, false, List.of(delta));

        RegressionReport report = new RegressionReport(
                0.8, 0.3, -0.5, Map.of(), List.of(reg), 1, 0);

        // Baseline has short output, current has much longer output
        EvalResult baseline = EvalResult.of(List.of(
                makeCaseResult("q1", "short", 0.8, true)), 1000L);
        EvalResult current = EvalResult.of(List.of(
                makeCaseResult("q1", "a very much longer output than before wow",
                        0.3, false)), 1000L);

        RootCauseReport rootCause = RootCauseAnalyzer.analyze(report, baseline, current);

        List<FailurePattern> patterns = rootCause.clusters().get(0).patterns();
        boolean hasOutputLengthPattern = patterns.stream()
                .anyMatch(p -> p.type() == PatternType.OUTPUT_LENGTH_CHANGE);
        assertTrue(hasOutputLengthPattern, "Should detect output length change");
    }

    @Test
    void analyzeDetectsToolUsageChange() {
        MetricDelta delta = new MetricDelta("Accuracy", 0.8, 0.3, -0.5);
        CaseStatusChange reg = new CaseStatusChange(
                "q1", true, false, List.of(delta));

        RegressionReport report = new RegressionReport(
                0.8, 0.3, -0.5, Map.of(), List.of(reg), 1, 0);

        EvalResult baseline = EvalResult.of(List.of(
                makeCaseResultWithTools("q1", "baseline", 0.8, true,
                        List.of(ToolCall.of("search")))), 1000L);
        EvalResult current = EvalResult.of(List.of(
                makeCaseResultWithTools("q1", "current", 0.3, false,
                        List.of(ToolCall.of("calculate")))), 1000L);

        RootCauseReport rootCause = RootCauseAnalyzer.analyze(report, baseline, current);

        List<FailurePattern> patterns = rootCause.clusters().get(0).patterns();
        boolean hasToolPattern = patterns.stream()
                .anyMatch(p -> p.type() == PatternType.TOOL_USAGE_CHANGE);
        assertTrue(hasToolPattern, "Should detect tool usage change");
    }

    @Test
    void analyzeDetectsCostIncrease() {
        MetricDelta delta = new MetricDelta("Accuracy", 0.8, 0.3, -0.5);
        CaseStatusChange reg = new CaseStatusChange(
                "q1", true, false, List.of(delta));

        RegressionReport report = new RegressionReport(
                0.8, 0.3, -0.5, Map.of(), List.of(reg), 1, 0);

        EvalResult baseline = EvalResult.of(List.of(
                makeCaseResultWithCostAndLatency("q1", "baseline", 0.8, true,
                        new BigDecimal("0.01"), 100L)), 1000L);
        EvalResult current = EvalResult.of(List.of(
                makeCaseResultWithCostAndLatency("q1", "current", 0.3, false,
                        new BigDecimal("0.05"), 100L)), 1000L);

        RootCauseReport rootCause = RootCauseAnalyzer.analyze(report, baseline, current);

        List<FailurePattern> patterns = rootCause.clusters().get(0).patterns();
        boolean hasCostPattern = patterns.stream()
                .anyMatch(p -> p.type() == PatternType.COST_INCREASE);
        assertTrue(hasCostPattern, "Should detect cost increase");
    }

    @Test
    void analyzeDetectsLatencyIncrease() {
        MetricDelta delta = new MetricDelta("Accuracy", 0.8, 0.3, -0.5);
        CaseStatusChange reg = new CaseStatusChange(
                "q1", true, false, List.of(delta));

        RegressionReport report = new RegressionReport(
                0.8, 0.3, -0.5, Map.of(), List.of(reg), 1, 0);

        EvalResult baseline = EvalResult.of(List.of(
                makeCaseResultWithCostAndLatency("q1", "baseline", 0.8, true,
                        new BigDecimal("0.01"), 100L)), 1000L);
        EvalResult current = EvalResult.of(List.of(
                makeCaseResultWithCostAndLatency("q1", "current", 0.3, false,
                        new BigDecimal("0.01"), 500L)), 1000L);

        RootCauseReport rootCause = RootCauseAnalyzer.analyze(report, baseline, current);

        List<FailurePattern> patterns = rootCause.clusters().get(0).patterns();
        boolean hasLatencyPattern = patterns.stream()
                .anyMatch(p -> p.type() == PatternType.LATENCY_INCREASE);
        assertTrue(hasLatencyPattern, "Should detect latency increase");
    }

    @Test
    void analyzeClustersRankedByImpactScore() {
        // Cluster 1: 2 cases, small delta
        MetricDelta smallDelta = new MetricDelta("Accuracy", 0.8, 0.7, -0.1);
        CaseStatusChange reg1 = new CaseStatusChange(
                "q1", true, false, List.of(smallDelta));
        CaseStatusChange reg2 = new CaseStatusChange(
                "q2", true, false, List.of(smallDelta));

        // Cluster 2: 1 case, large delta on different metric
        MetricDelta largeDelta = new MetricDelta("Faithfulness", 0.9, 0.1, -0.8);
        CaseStatusChange reg3 = new CaseStatusChange(
                "q3", true, false, List.of(largeDelta));

        RegressionReport report = new RegressionReport(
                0.85, 0.43, -0.42, Map.of(),
                List.of(reg1, reg2, reg3), 3, 0);

        EvalResult baseline = EvalResult.of(List.of(
                makeCaseResult("q1", "b1", 0.8, true),
                makeCaseResult("q2", "b2", 0.8, true),
                makeCaseResult("q3", "b3", 0.9, true)), 1000L);
        EvalResult current = EvalResult.of(List.of(
                makeCaseResult("q1", "c1", 0.7, false),
                makeCaseResult("q2", "c2", 0.7, false),
                makeCaseResult("q3", "c3", 0.1, false)), 1000L);

        RootCauseReport rootCause = RootCauseAnalyzer.analyze(report, baseline, current);

        assertEquals(2, rootCause.clusters().size());
        // Faithfulness cluster: |0.8| * 1 = 0.8
        // Accuracy cluster: |0.1| * 2 = 0.2
        // Faithfulness should be ranked first
        assertEquals("Faithfulness", rootCause.clusters().get(0).clusterName());
        assertEquals("Accuracy", rootCause.clusters().get(1).clusterName());
    }

    @Test
    void analyzeSummaryContainsKeyInfo() {
        MetricDelta delta = new MetricDelta("Accuracy", 0.8, 0.3, -0.5);
        CaseStatusChange reg = new CaseStatusChange(
                "q1", true, false, List.of(delta));

        RegressionReport report = new RegressionReport(
                0.8, 0.3, -0.5, Map.of(), List.of(reg), 1, 0);

        EvalResult baseline = EvalResult.of(List.of(
                makeCaseResult("q1", "baseline", 0.8, true)), 1000L);
        EvalResult current = EvalResult.of(List.of(
                makeCaseResult("q1", "current", 0.3, false)), 1000L);

        RootCauseReport rootCause = RootCauseAnalyzer.analyze(report, baseline, current);

        assertNotNull(rootCause.summary());
        assertFalse(rootCause.summary().isEmpty());
        assertTrue(rootCause.summary().contains("1 regressed"));
        assertTrue(rootCause.summary().contains("1 clusters"));
    }

    @Test
    void analyzeRejectsNullReport() {
        EvalResult result = EvalResult.of(List.of(), 0L);
        assertThrows(NullPointerException.class,
                () -> RootCauseAnalyzer.analyze(null, result, result));
    }

    @Test
    void analyzeRejectsNullBaseline() {
        RegressionReport report = new RegressionReport(
                0.8, 0.8, 0.0, Map.of(), List.of(), 0, 0);
        EvalResult result = EvalResult.of(List.of(), 0L);
        assertThrows(NullPointerException.class,
                () -> RootCauseAnalyzer.analyze(report, null, result));
    }

    @Test
    void analyzeRejectsNullCurrent() {
        RegressionReport report = new RegressionReport(
                0.8, 0.8, 0.0, Map.of(), List.of(), 0, 0);
        EvalResult result = EvalResult.of(List.of(), 0L);
        assertThrows(NullPointerException.class,
                () -> RootCauseAnalyzer.analyze(report, result, null));
    }
}
