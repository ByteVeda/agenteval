package org.byteveda.agenteval.metrics.cost;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single point on the cost-quality trade-off space.
 *
 * @param variantName name of the model/configuration variant
 * @param averageScore average evaluation score (0.0-1.0)
 * @param totalCost total cost in USD
 * @param paretoOptimal whether this point is on the Pareto frontier
 */
public record ParetoPoint(
        String variantName,
        double averageScore,
        BigDecimal totalCost,
        boolean paretoOptimal
) {
    public ParetoPoint {
        Objects.requireNonNull(variantName, "variantName must not be null");
        Objects.requireNonNull(totalCost, "totalCost must not be null");
    }
}
