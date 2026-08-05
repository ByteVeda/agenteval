package org.byteveda.agenteval.fingerprint;

import org.byteveda.agenteval.core.eval.AgentEval;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Profiles an agent's capabilities across multiple dimensions.
 *
 * <p>Runs targeted benchmarks for each configured dimension and aggregates
 * the results into a {@link CapabilityProfile}.</p>
 *
 * <pre>{@code
 * var profile = CapabilityProfiler.builder()
 *     .agentName("my-agent-v2")
 *     .addBenchmark(new DimensionBenchmark(
 *         CapabilityDimension.ACCURACY,
 *         List.of(new CorrectnessMetric(judgeProvider, 0.7)),
 *         accuracyTestCases
 *     ))
 *     .build()
 *     .profile();
 * }</pre>
 */
public final class CapabilityProfiler {

    private static final Logger LOG = LoggerFactory.getLogger(CapabilityProfiler.class);

    private final String agentName;
    private final List<DimensionBenchmark> benchmarks;

    private CapabilityProfiler(Builder builder) {
        this.agentName = builder.agentName;
        this.benchmarks = List.copyOf(builder.benchmarks);
    }

    /**
     * Creates a new builder.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs all benchmarks and builds a capability profile.
     *
     * @return the capability profile
     */
    public CapabilityProfile profile() {
        LOG.info("Profiling agent '{}' across {} dimensions",
                agentName, benchmarks.size());
        long startTime = System.currentTimeMillis();

        Map<CapabilityDimension, ProfileScore> scores = new LinkedHashMap<>();

        for (DimensionBenchmark benchmark : benchmarks) {
            ProfileScore score = evaluateDimension(benchmark);
            scores.put(benchmark.dimension(), score);
            LOG.info("Dimension '{}': {}",
                    benchmark.dimension().displayName(), score.score());
        }

        long durationMs = System.currentTimeMillis() - startTime;
        LOG.info("Profiling complete in {}ms", durationMs);

        return new CapabilityProfile(agentName, scores, durationMs);
    }

    private ProfileScore evaluateDimension(DimensionBenchmark benchmark) {
        List<EvalMetric> metrics = benchmark.metrics();
        List<AgentTestCase> testCases = benchmark.testCases();

        EvalResult result = AgentEval.evaluate(testCases, metrics);
        double avgScore = result.averageScore();

        String reason = String.format(
                "Average across %d test cases and %d metrics",
                testCases.size(), metrics.size()
        );

        return new ProfileScore(benchmark.dimension(), avgScore, reason);
    }

    /**
     * Builder for {@link CapabilityProfiler}.
     */
    public static final class Builder {

        private String agentName;
        private final List<DimensionBenchmark> benchmarks = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the agent name for the profile.
         */
        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        /**
         * Adds a dimension benchmark.
         */
        public Builder addBenchmark(DimensionBenchmark benchmark) {
            this.benchmarks.add(
                    Objects.requireNonNull(benchmark, "benchmark must not be null")
            );
            return this;
        }

        /**
         * Builds the profiler.
         *
         * @return a new {@link CapabilityProfiler}
         * @throws NullPointerException     if agentName is null
         * @throws IllegalArgumentException if no benchmarks are configured
         */
        public CapabilityProfiler build() {
            Objects.requireNonNull(agentName, "agentName must not be null");
            if (benchmarks.isEmpty()) {
                throw new IllegalArgumentException(
                        "at least one benchmark is required"
                );
            }
            return new CapabilityProfiler(this);
        }
    }
}
