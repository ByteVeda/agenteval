package org.byteveda.agenteval.metrics.cost;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Summary of cost efficiency for an evaluation run.
 *
 * @param totalCost total cost in USD across all cases
 * @param costPerCase average cost per test case
 * @param costPerPassingCase average cost per passing test case (or null if none passed)
 * @param costEfficiencyRatio score-per-dollar ratio (higher is better)
 * @param passRate fraction of cases that passed (0.0-1.0)
 * @param averageScore average evaluation score (0.0-1.0)
 */
public record CostEfficiencyReport(
        BigDecimal totalCost,
        BigDecimal costPerCase,
        BigDecimal costPerPassingCase,
        double costEfficiencyRatio,
        double passRate,
        double averageScore
) {
    public CostEfficiencyReport {
        Objects.requireNonNull(totalCost, "totalCost must not be null");
        Objects.requireNonNull(costPerCase, "costPerCase must not be null");
        Objects.requireNonNull(costPerPassingCase, "costPerPassingCase must not be null");
    }
}
