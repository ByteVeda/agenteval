package org.byteveda.agenteval.reporting.regression.rootcause;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.ToolCall;
import org.byteveda.agenteval.reporting.regression.CaseStatusChange;
import org.byteveda.agenteval.reporting.regression.MetricDelta;
import org.byteveda.agenteval.reporting.regression.RegressionReport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Analyzes regression reports to identify root causes by clustering regressed cases
 * and detecting common failure patterns.
 *
 * <p>Thread-safe: all methods are stateless.</p>
 */
public final class RootCauseAnalyzer {

    private RootCauseAnalyzer() {
        // utility class
    }

    /**
     * Performs root cause analysis on a regression report.
     *
     * <p>Steps:
     * <ol>
     *   <li>Filters regressed cases (newFailure) from the report</li>
     *   <li>Groups cases by the set of regressed metric names</li>
     *   <li>For each cluster, detects output length changes, tool usage changes,
     *       cost increases, and latency increases</li>
     *   <li>Ranks clusters by impactScore = |avgDelta| x clusterSize</li>
     *   <li>Generates a human-readable summary</li>
     * </ol>
     *
     * @param report the regression report
     * @param baseline the baseline evaluation result
     * @param current the current evaluation result
     * @return the root cause analysis report
     */
    public static RootCauseReport analyze(RegressionReport report, EvalResult baseline,
            EvalResult current) {
        Objects.requireNonNull(report, "report must not be null");
        Objects.requireNonNull(baseline, "baseline must not be null");
        Objects.requireNonNull(current, "current must not be null");

        List<CaseStatusChange> regressed = report.caseChanges().stream()
                .filter(CaseStatusChange::newFailure)
                .toList();

        if (regressed.isEmpty()) {
            return new RootCauseReport(List.of(), "No regressions detected.", 0);
        }

        Map<String, CaseResult> baselineByInput = indexByInput(baseline);
        Map<String, CaseResult> currentByInput = indexByInput(current);

        // Group by set of regressed metric names
        Map<Set<String>, List<CaseStatusChange>> groups = regressed.stream()
                .collect(Collectors.groupingBy(
                        RootCauseAnalyzer::regressedMetricNames,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<RegressionCluster> clusters = new ArrayList<>();
        for (Map.Entry<Set<String>, List<CaseStatusChange>> entry : groups.entrySet()) {
            Set<String> metricNames = entry.getKey();
            List<CaseStatusChange> cases = entry.getValue();

            String clusterName = metricNames.isEmpty()
                    ? "Unknown regression"
                    : String.join(", ", metricNames);

            List<FailurePattern> patterns = detectPatterns(
                    cases, metricNames, baselineByInput, currentByInput);

            double avgDelta = cases.stream()
                    .flatMap(c -> c.metricDeltas().stream())
                    .filter(MetricDelta::regressed)
                    .mapToDouble(d -> Math.abs(d.delta()))
                    .average()
                    .orElse(0.0);

            double impactScore = avgDelta * cases.size();

            clusters.add(new RegressionCluster(clusterName, cases, impactScore, patterns));
        }

        // Sort by impact score descending
        clusters.sort(Comparator.comparingDouble(RegressionCluster::impactScore).reversed());

        String summary = buildSummary(clusters, regressed.size());

        return new RootCauseReport(clusters, summary, regressed.size());
    }

    private static Set<String> regressedMetricNames(CaseStatusChange change) {
        return change.metricDeltas().stream()
                .filter(MetricDelta::regressed)
                .map(MetricDelta::metricName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static List<FailurePattern> detectPatterns(
            List<CaseStatusChange> cases,
            Set<String> regressedMetrics,
            Map<String, CaseResult> baselineByInput,
            Map<String, CaseResult> currentByInput) {

        List<FailurePattern> patterns = new ArrayList<>();

        // Pattern: Metric regression
        for (String metric : regressedMetrics) {
            double avgDelta = cases.stream()
                    .flatMap(c -> c.metricDeltas().stream())
                    .filter(d -> metric.equals(d.metricName()))
                    .mapToDouble(MetricDelta::delta)
                    .average()
                    .orElse(0.0);

            if (avgDelta < 0) {
                patterns.add(new FailurePattern(
                        PatternType.METRIC_REGRESSION,
                        String.format("Metric '%s' regressed by avg %.3f across %d cases",
                                metric, Math.abs(avgDelta), cases.size()),
                        Math.abs(avgDelta)));
            }
        }

        // Pattern: Output length change
        detectOutputLengthChanges(cases, baselineByInput, currentByInput)
                .ifPresent(patterns::add);

        // Pattern: Tool usage change
        detectToolUsageChanges(cases, baselineByInput, currentByInput)
                .ifPresent(patterns::add);

        // Pattern: Cost increase
        detectCostIncrease(cases, baselineByInput, currentByInput)
                .ifPresent(patterns::add);

        // Pattern: Latency increase
        detectLatencyIncrease(cases, baselineByInput, currentByInput)
                .ifPresent(patterns::add);

        return patterns;
    }

    private static Optional<FailurePattern> detectOutputLengthChanges(
            List<CaseStatusChange> cases,
            Map<String, CaseResult> baselineByInput,
            Map<String, CaseResult> currentByInput) {

        double totalRatio = 0.0;
        int count = 0;

        for (CaseStatusChange change : cases) {
            CaseResult bl = baselineByInput.get(change.input());
            CaseResult cr = currentByInput.get(change.input());
            if (bl == null || cr == null) continue;

            String blOutput = bl.testCase().getActualOutput();
            String crOutput = cr.testCase().getActualOutput();
            if (blOutput == null || crOutput == null) continue;

            int blLen = blOutput.length();
            int crLen = crOutput.length();
            if (blLen == 0) continue;

            totalRatio += (double) (crLen - blLen) / blLen;
            count++;
        }

        if (count == 0) return Optional.empty();

        double avgRatio = totalRatio / count;
        if (Math.abs(avgRatio) < 0.1) return Optional.empty(); // less than 10% change

        String direction = avgRatio > 0 ? "increased" : "decreased";
        return Optional.of(new FailurePattern(
                PatternType.OUTPUT_LENGTH_CHANGE,
                String.format("Output length %s by avg %.0f%% across %d cases",
                        direction, Math.abs(avgRatio) * 100, count),
                Math.abs(avgRatio)));
    }

    private static Optional<FailurePattern> detectToolUsageChanges(
            List<CaseStatusChange> cases,
            Map<String, CaseResult> baselineByInput,
            Map<String, CaseResult> currentByInput) {

        int changedCount = 0;

        for (CaseStatusChange change : cases) {
            CaseResult bl = baselineByInput.get(change.input());
            CaseResult cr = currentByInput.get(change.input());
            if (bl == null || cr == null) continue;

            List<String> blTools = bl.testCase().getToolCalls().stream()
                    .map(ToolCall::name)
                    .sorted()
                    .toList();
            List<String> crTools = cr.testCase().getToolCalls().stream()
                    .map(ToolCall::name)
                    .sorted()
                    .toList();

            if (!blTools.equals(crTools)) {
                changedCount++;
            }
        }

        if (changedCount == 0) return Optional.empty();

        double ratio = (double) changedCount / cases.size();
        return Optional.of(new FailurePattern(
                PatternType.TOOL_USAGE_CHANGE,
                String.format("Tool usage changed in %d/%d regressed cases (%.0f%%)",
                        changedCount, cases.size(), ratio * 100),
                ratio));
    }

    private static Optional<FailurePattern> detectCostIncrease(
            List<CaseStatusChange> cases,
            Map<String, CaseResult> baselineByInput,
            Map<String, CaseResult> currentByInput) {

        double totalRatio = 0.0;
        int count = 0;

        for (CaseStatusChange change : cases) {
            CaseResult bl = baselineByInput.get(change.input());
            CaseResult cr = currentByInput.get(change.input());
            if (bl == null || cr == null) continue;

            BigDecimal blCost = bl.testCase().getCost();
            BigDecimal crCost = cr.testCase().getCost();
            if (blCost == null || crCost == null) continue;
            if (blCost.compareTo(BigDecimal.ZERO) == 0) continue;

            double ratio = (crCost.doubleValue() - blCost.doubleValue())
                    / blCost.doubleValue();
            totalRatio += ratio;
            count++;
        }

        if (count == 0) return Optional.empty();

        double avgRatio = totalRatio / count;
        if (avgRatio < 0.1) return Optional.empty(); // less than 10% increase

        return Optional.of(new FailurePattern(
                PatternType.COST_INCREASE,
                String.format("Cost increased by avg %.0f%% across %d cases",
                        avgRatio * 100, count),
                avgRatio));
    }

    private static Optional<FailurePattern> detectLatencyIncrease(
            List<CaseStatusChange> cases,
            Map<String, CaseResult> baselineByInput,
            Map<String, CaseResult> currentByInput) {

        double totalRatio = 0.0;
        int count = 0;

        for (CaseStatusChange change : cases) {
            CaseResult bl = baselineByInput.get(change.input());
            CaseResult cr = currentByInput.get(change.input());
            if (bl == null || cr == null) continue;

            long blLatency = bl.testCase().getLatencyMs();
            long crLatency = cr.testCase().getLatencyMs();
            if (blLatency <= 0) continue;

            double ratio = (double) (crLatency - blLatency) / blLatency;
            totalRatio += ratio;
            count++;
        }

        if (count == 0) return Optional.empty();

        double avgRatio = totalRatio / count;
        if (avgRatio < 0.1) return Optional.empty(); // less than 10% increase

        return Optional.of(new FailurePattern(
                PatternType.LATENCY_INCREASE,
                String.format("Latency increased by avg %.0f%% across %d cases",
                        avgRatio * 100, count),
                avgRatio));
    }

    private static Map<String, CaseResult> indexByInput(EvalResult result) {
        Map<String, CaseResult> index = new LinkedHashMap<>();
        for (CaseResult cr : result.caseResults()) {
            index.put(cr.testCase().getInput(), cr);
        }
        return index;
    }

    private static String buildSummary(List<RegressionCluster> clusters, int totalRegressed) {
        if (clusters.isEmpty()) {
            return "No regression clusters identified.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Analyzed %d regressed cases in %d clusters. ",
                totalRegressed, clusters.size()));

        RegressionCluster top = clusters.get(0);
        sb.append(String.format("Highest-impact cluster: '%s' (%d cases, impact=%.3f). ",
                top.clusterName(), top.cases().size(), top.impactScore()));

        long patternCount = clusters.stream()
                .flatMap(c -> c.patterns().stream())
                .count();
        sb.append(String.format("Total patterns detected: %d.", patternCount));

        return sb.toString();
    }
}
