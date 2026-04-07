package org.byteveda.agenteval.metrics.cost;

import java.util.List;

/**
 * The Pareto frontier computed from a set of variant evaluation results.
 *
 * @param points all evaluated points, with {@code paretoOptimal} flag set
 * @param dominatedVariants names of variants that are dominated (not Pareto-optimal)
 */
public record ParetoFrontier(
        List<ParetoPoint> points,
        List<String> dominatedVariants
) {
    public ParetoFrontier {
        points = List.copyOf(points);
        dominatedVariants = List.copyOf(dominatedVariants);
    }
}
