package org.byteveda.agenteval.core.benchmark;

import org.byteveda.agenteval.core.config.AgentEvalConfig;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A named evaluation variant for benchmarking.
 *
 * <p>Each variant can specify its own config, metrics, and a case preparer
 * that transforms test cases before evaluation (e.g., to call a different model).</p>
 *
 * <pre>{@code
 * var variant = BenchmarkVariant.builder()
 *     .name("gpt-4o")
 *     .metrics(List.of(new AnswerRelevancyMetric(judge)))
 *     .casePreparer(tc -> tc.toBuilder().actualOutput(callGpt4o(tc.getInput())).build())
 *     .build();
 * }</pre>
 */
public final class BenchmarkVariant {

    private final String name;
    private final AgentEvalConfig config;
    private final List<EvalMetric> metrics;
    private final UnaryOperator<AgentTestCase> casePreparer;

    private BenchmarkVariant(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name must not be null");
        if (builder.name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (builder.metrics == null || builder.metrics.isEmpty()) {
            throw new IllegalArgumentException("metrics must not be null or empty");
        }
        this.config = builder.config;
        this.metrics = List.copyOf(builder.metrics);
        this.casePreparer = builder.casePreparer;
    }

    public String name() { return name; }
    public AgentEvalConfig config() { return config; }
    public List<EvalMetric> metrics() { return metrics; }
    public UnaryOperator<AgentTestCase> casePreparer() { return casePreparer; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private AgentEvalConfig config = AgentEvalConfig.defaults();
        private List<EvalMetric> metrics;
        private UnaryOperator<AgentTestCase> casePreparer = UnaryOperator.identity();

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }
        public Builder config(AgentEvalConfig config) { this.config = config; return this; }
        public Builder metrics(List<EvalMetric> metrics) { this.metrics = metrics; return this; }
        public Builder casePreparer(UnaryOperator<AgentTestCase> preparer) {
            this.casePreparer = preparer;
            return this;
        }

        public BenchmarkVariant build() {
            return new BenchmarkVariant(this);
        }
    }
}
