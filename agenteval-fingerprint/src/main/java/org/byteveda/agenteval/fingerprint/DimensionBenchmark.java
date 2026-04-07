package org.byteveda.agenteval.fingerprint;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;

import java.util.List;
import java.util.Objects;

/**
 * Associates a capability dimension with its evaluation metrics and test cases.
 *
 * @param dimension the capability dimension being benchmarked
 * @param metrics   the metrics used to evaluate this dimension
 * @param testCases the test cases for this dimension
 */
public record DimensionBenchmark(
        CapabilityDimension dimension,
        List<EvalMetric> metrics,
        List<AgentTestCase> testCases
) {

    public DimensionBenchmark {
        Objects.requireNonNull(dimension, "dimension must not be null");
        Objects.requireNonNull(metrics, "metrics must not be null");
        Objects.requireNonNull(testCases, "testCases must not be null");
        metrics = List.copyOf(metrics);
        testCases = List.copyOf(testCases);
    }
}
