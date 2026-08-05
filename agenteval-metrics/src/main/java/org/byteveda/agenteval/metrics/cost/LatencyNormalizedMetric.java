package org.byteveda.agenteval.metrics.cost;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;

import java.util.Objects;

/**
 * Wraps a base metric and adjusts its score by latency relative to a reference latency.
 *
 * <p>If actual latency is lower than the reference, the normalized score may exceed the base
 * score (clamped to 1.0). If actual latency is higher, the score is penalized proportionally.</p>
 *
 * <p>Thread-safe: delegates to the base metric which must itself be thread-safe.</p>
 */
public final class LatencyNormalizedMetric implements EvalMetric {

    private final EvalMetric baseMetric;
    private final long referenceLatencyMs;
    private final double threshold;

    /**
     * @param baseMetric the underlying metric to evaluate
     * @param referenceLatencyMs the reference (budget) latency in milliseconds
     * @param threshold the pass/fail threshold for the normalized score
     */
    public LatencyNormalizedMetric(EvalMetric baseMetric, long referenceLatencyMs,
            double threshold) {
        this.baseMetric = Objects.requireNonNull(baseMetric, "baseMetric must not be null");
        if (referenceLatencyMs <= 0) {
            throw new IllegalArgumentException(
                    "referenceLatencyMs must be positive, got: " + referenceLatencyMs);
        }
        this.referenceLatencyMs = referenceLatencyMs;
        this.threshold = threshold;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        EvalScore baseScore = baseMetric.evaluate(testCase);
        long actualLatency = testCase.getLatencyMs();

        if (actualLatency <= 0) {
            return EvalScore.of(baseScore.value(), threshold,
                    "Latency data unavailable, using base score. " + baseScore.reason());
        }

        double latencyRatio = (double) referenceLatencyMs / actualLatency;
        double normalized = Math.min(1.0, Math.max(0.0, baseScore.value() * latencyRatio));

        return EvalScore.of(normalized, threshold,
                String.format("Base=%.3f, latency=%dms, ref=%dms, normalized=%.3f",
                        baseScore.value(), actualLatency,
                        referenceLatencyMs, normalized));
    }

    @Override
    public String name() {
        return baseMetric.name() + "/LatencyNormalized";
    }
}
