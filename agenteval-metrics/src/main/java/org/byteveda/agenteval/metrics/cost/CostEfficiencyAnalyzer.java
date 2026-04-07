package org.byteveda.agenteval.metrics.cost;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Static utility for computing cost efficiency reports and Pareto frontiers
 * across evaluation runs.
 *
 * <p>Thread-safe: all methods are stateless.</p>
 */
public final class CostEfficiencyAnalyzer {

    private CostEfficiencyAnalyzer() {
        // utility class
    }

    /**
     * Analyzes cost efficiency of a single evaluation result.
     *
     * <p>Sums per-case costs from {@link CaseResult#testCase()}'s cost field.
     * If no cost data is available, totals default to zero.</p>
     *
     * @param result the evaluation result to analyze
     * @return a cost efficiency report
     */
    public static CostEfficiencyReport analyze(EvalResult result) {
        Objects.requireNonNull(result, "result must not be null");

        List<CaseResult> cases = result.caseResults();
        BigDecimal totalCost = BigDecimal.ZERO;

        for (CaseResult cr : cases) {
            BigDecimal caseCost = cr.testCase().getCost();
            if (caseCost != null) {
                totalCost = totalCost.add(caseCost);
            }
        }

        int totalCases = cases.size();
        long passedCount = cases.stream().filter(CaseResult::passed).count();
        double passRate = totalCases == 0 ? 0.0 : (double) passedCount / totalCases;
        double averageScore = result.averageScore();

        BigDecimal costPerCase = totalCases == 0
                ? BigDecimal.ZERO
                : totalCost.divide(BigDecimal.valueOf(totalCases), 10, RoundingMode.HALF_UP);

        BigDecimal costPerPassingCase = passedCount == 0
                ? BigDecimal.ZERO
                : totalCost.divide(BigDecimal.valueOf(passedCount), 10, RoundingMode.HALF_UP);

        double costEfficiencyRatio = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : averageScore / totalCost.doubleValue();

        return new CostEfficiencyReport(
                totalCost, costPerCase, costPerPassingCase,
                costEfficiencyRatio, passRate, averageScore);
    }

    /**
     * Computes the Pareto frontier from multiple named evaluation variants.
     *
     * <p>A variant is Pareto-optimal if no other variant has both a higher average
     * score and a lower total cost.</p>
     *
     * @param variantResults map of variant name to evaluation result
     * @return the Pareto frontier with all points and dominated variant names
     */
    public static ParetoFrontier paretoFrontier(Map<String, EvalResult> variantResults) {
        Objects.requireNonNull(variantResults, "variantResults must not be null");

        List<ParetoPoint> candidates = new ArrayList<>();
        for (Map.Entry<String, EvalResult> entry : variantResults.entrySet()) {
            String name = entry.getKey();
            EvalResult result = entry.getValue();
            BigDecimal totalCost = computeTotalCost(result);
            double avgScore = result.averageScore();
            candidates.add(new ParetoPoint(name, avgScore, totalCost, false));
        }

        List<ParetoPoint> points = new ArrayList<>();
        List<String> dominated = new ArrayList<>();

        for (ParetoPoint candidate : candidates) {
            boolean isDominated = false;
            for (ParetoPoint other : candidates) {
                if (other == candidate) continue;
                if (dominates(other, candidate)) {
                    isDominated = true;
                    break;
                }
            }
            points.add(new ParetoPoint(
                    candidate.variantName(),
                    candidate.averageScore(),
                    candidate.totalCost(),
                    !isDominated));
            if (isDominated) {
                dominated.add(candidate.variantName());
            }
        }

        return new ParetoFrontier(points, dominated);
    }

    /**
     * Returns true if {@code a} dominates {@code b}: a has equal-or-higher score
     * AND equal-or-lower cost, with at least one strict inequality.
     */
    private static boolean dominates(ParetoPoint a, ParetoPoint b) {
        boolean scoreAtLeast = a.averageScore() >= b.averageScore();
        boolean costAtMost = a.totalCost().compareTo(b.totalCost()) <= 0;
        boolean strictlyBetterScore = a.averageScore() > b.averageScore();
        boolean strictlyBetterCost = a.totalCost().compareTo(b.totalCost()) < 0;
        return scoreAtLeast && costAtMost && (strictlyBetterScore || strictlyBetterCost);
    }

    private static BigDecimal computeTotalCost(EvalResult result) {
        BigDecimal total = BigDecimal.ZERO;
        for (CaseResult cr : result.caseResults()) {
            BigDecimal caseCost = cr.testCase().getCost();
            if (caseCost != null) {
                total = total.add(caseCost);
            }
        }
        return total;
    }
}
