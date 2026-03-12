package com.agenteval.core.eval;

import com.agenteval.core.cost.CostSummary;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregated evaluation result across all test cases and metrics.
 */
public final class EvalResult {

    private final List<CaseResult> caseResults;
    private final long durationMs;
    private final CostSummary costSummary;

    private EvalResult(List<CaseResult> caseResults, long durationMs, CostSummary costSummary) {
        Objects.requireNonNull(caseResults, "caseResults must not be null");
        this.caseResults = List.copyOf(caseResults);
        this.durationMs = durationMs;
        this.costSummary = costSummary;
    }

    public static EvalResult of(List<CaseResult> caseResults, long durationMs) {
        return new EvalResult(caseResults, durationMs, null);
    }

    public static EvalResult of(List<CaseResult> caseResults, long durationMs,
                                CostSummary costSummary) {
        return new EvalResult(caseResults, durationMs, costSummary);
    }

    public List<CaseResult> caseResults() { return caseResults; }
    public long durationMs() { return durationMs; }
    public CostSummary costSummary() { return costSummary; }

    /**
     * Returns the overall average score across all cases and metrics.
     */
    public double averageScore() {
        return caseResults.stream()
                .mapToDouble(CaseResult::averageScore)
                .average()
                .orElse(0.0);
    }

    /**
     * Returns the pass rate as a percentage (0.0–1.0).
     */
    public double passRate() {
        if (caseResults.isEmpty()) return 0.0;
        long passed = caseResults.stream().filter(CaseResult::passed).count();
        return (double) passed / caseResults.size();
    }

    /**
     * Returns the test cases that failed at least one metric.
     */
    public List<CaseResult> failedCases() {
        return caseResults.stream()
                .filter(cr -> !cr.passed())
                .toList();
    }

    /**
     * Returns average scores per metric across all cases.
     */
    public Map<String, Double> averageScoresByMetric() {
        return caseResults.stream()
                .flatMap(cr -> cr.scores().entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.averagingDouble(e -> e.getValue().value())
                ));
    }

    /**
     * Prints a summary to stdout.
     */
    public void summary() {
        System.out.println("=== AgentEval Results ===");
        System.out.printf("Cases: %d | Passed: %d | Failed: %d | Pass Rate: %.1f%%%n",
                caseResults.size(),
                caseResults.size() - failedCases().size(),
                failedCases().size(),
                passRate() * 100);
        System.out.printf("Average Score: %.3f | Duration: %dms%n", averageScore(), durationMs);

        var byMetric = averageScoresByMetric();
        if (!byMetric.isEmpty()) {
            System.out.println("--- Per-Metric Averages ---");
            byMetric.forEach((name, avg) ->
                    System.out.printf("  %-30s %.3f%n", name, avg));
        }
    }
}
