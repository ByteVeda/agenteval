package org.byteveda.agenteval.metrics.cost;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Wraps a base metric and adjusts its score by cost relative to a reference budget.
 *
 * <p>If actual cost is lower than the reference, the normalized score may exceed the base
 * score (clamped to 1.0). If actual cost is higher, the score is penalized proportionally.</p>
 *
 * <p>Thread-safe: delegates to the base metric which must itself be thread-safe.</p>
 */
public final class CostNormalizedMetric implements EvalMetric {

    private final EvalMetric baseMetric;
    private final BigDecimal referenceCostUsd;
    private final double threshold;

    /**
     * @param baseMetric the underlying metric to evaluate
     * @param referenceCostUsd the reference (budget) cost in USD
     * @param threshold the pass/fail threshold for the normalized score
     */
    public CostNormalizedMetric(EvalMetric baseMetric, BigDecimal referenceCostUsd,
            double threshold) {
        this.baseMetric = Objects.requireNonNull(baseMetric, "baseMetric must not be null");
        this.referenceCostUsd = Objects.requireNonNull(referenceCostUsd,
                "referenceCostUsd must not be null");
        if (referenceCostUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "referenceCostUsd must be positive, got: " + referenceCostUsd);
        }
        this.threshold = threshold;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        EvalScore baseScore = baseMetric.evaluate(testCase);
        BigDecimal actualCost = testCase.getCost();

        if (actualCost == null || actualCost.compareTo(BigDecimal.ZERO) <= 0) {
            return EvalScore.of(baseScore.value(), threshold,
                    "Cost data unavailable, using base score. " + baseScore.reason());
        }

        double costRatio = referenceCostUsd.doubleValue() / actualCost.doubleValue();
        double normalized = Math.min(1.0, Math.max(0.0, baseScore.value() * costRatio));

        return EvalScore.of(normalized, threshold,
                String.format("Base=%.3f, cost=$%.6f, ref=$%.6f, normalized=%.3f",
                        baseScore.value(), actualCost.doubleValue(),
                        referenceCostUsd.doubleValue(), normalized));
    }

    @Override
    public String name() {
        return baseMetric.name() + "/CostNormalized";
    }
}
