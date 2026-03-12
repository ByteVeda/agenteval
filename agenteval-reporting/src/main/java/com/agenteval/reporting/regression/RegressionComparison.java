package com.agenteval.reporting.regression;

import com.agenteval.core.eval.CaseResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.core.model.EvalScore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compares two evaluation results (baseline vs current) and produces a regression report.
 *
 * <p>Matches test cases by input text.</p>
 */
public final class RegressionComparison {

    private RegressionComparison() {}

    /**
     * Compares baseline and current evaluation results.
     *
     * @param baseline the baseline (previous) result
     * @param current the current result
     * @return a regression report highlighting changes
     */
    public static RegressionReport compare(EvalResult baseline, EvalResult current) {
        Objects.requireNonNull(baseline, "baseline must not be null");
        Objects.requireNonNull(current, "current must not be null");

        Map<String, CaseResult> baselineByInput = indexByInput(baseline.caseResults());
        Map<String, CaseResult> currentByInput = indexByInput(current.caseResults());

        // Per-metric average deltas
        Map<String, Double> baselineAvgs = baseline.averageScoresByMetric();
        Map<String, Double> currentAvgs = current.averageScoresByMetric();
        Map<String, MetricDelta> metricDeltas = new LinkedHashMap<>();

        for (String metric : currentAvgs.keySet()) {
            double baseAvg = baselineAvgs.getOrDefault(metric, 0.0);
            double curAvg = currentAvgs.get(metric);
            metricDeltas.put(metric,
                    new MetricDelta(metric, baseAvg, curAvg, curAvg - baseAvg));
        }

        // Per-case changes (only for cases present in both runs)
        List<CaseStatusChange> caseChanges = new ArrayList<>();
        int newFailures = 0;
        int newPasses = 0;

        for (Map.Entry<String, CaseResult> entry : currentByInput.entrySet()) {
            CaseResult baseCase = baselineByInput.get(entry.getKey());
            if (baseCase == null) continue;

            CaseResult curCase = entry.getValue();
            List<MetricDelta> caseMetricDeltas = new ArrayList<>();

            for (Map.Entry<String, EvalScore> scoreEntry : curCase.scores().entrySet()) {
                String metric = scoreEntry.getKey();
                double curScore = scoreEntry.getValue().value();
                EvalScore baseScore = baseCase.scores().get(metric);
                double baseVal = baseScore != null ? baseScore.value() : 0.0;
                caseMetricDeltas.add(
                        new MetricDelta(metric, baseVal, curScore, curScore - baseVal));
            }

            var change = new CaseStatusChange(
                    entry.getKey(), baseCase.passed(), curCase.passed(), caseMetricDeltas);
            caseChanges.add(change);

            if (change.newFailure()) newFailures++;
            if (change.newPass()) newPasses++;
        }

        return new RegressionReport(
                baseline.averageScore(),
                current.averageScore(),
                current.averageScore() - baseline.averageScore(),
                metricDeltas,
                caseChanges,
                newFailures,
                newPasses);
    }

    private static Map<String, CaseResult> indexByInput(List<CaseResult> results) {
        Map<String, CaseResult> map = new LinkedHashMap<>();
        for (CaseResult cr : results) {
            map.put(cr.testCase().getInput(), cr);
        }
        return map;
    }
}
