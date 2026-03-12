package com.agenteval.core.metric;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Combines multiple metrics with configurable aggregation strategy.
 *
 * <pre>{@code
 * var composite = CompositeMetric.builder()
 *     .name("OverallQuality")
 *     .add(new AnswerRelevancy(), 0.4)
 *     .add(new Faithfulness(), 0.4)
 *     .add(new Conciseness(), 0.2)
 *     .strategy(CompositeStrategy.WEIGHTED_AVERAGE)
 *     .threshold(0.7)
 *     .build();
 * }</pre>
 */
public final class CompositeMetric implements EvalMetric {

    private final String name;
    private final List<WeightedMetric> metrics;
    private final CompositeStrategy strategy;
    private final double threshold;

    private CompositeMetric(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name must not be null");
        if (builder.metrics.isEmpty()) {
            throw new IllegalArgumentException("at least one metric must be added");
        }
        this.metrics = List.copyOf(builder.metrics);
        this.strategy = builder.strategy;
        this.threshold = builder.threshold;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        List<EvalScore> scores = metrics.stream()
                .map(wm -> wm.metric().evaluate(testCase))
                .toList();

        return switch (strategy) {
            case WEIGHTED_AVERAGE -> evaluateWeightedAverage(scores);
            case ALL_PASS -> evaluateAllPass(scores);
            case ANY_PASS -> evaluateAnyPass(scores);
        };
    }

    private EvalScore evaluateWeightedAverage(List<EvalScore> scores) {
        double totalWeight = metrics.stream().mapToDouble(WeightedMetric::weight).sum();
        double weightedSum = 0.0;
        for (int i = 0; i < scores.size(); i++) {
            weightedSum += scores.get(i).value() * metrics.get(i).weight();
        }
        double value = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        String reason = buildReason(scores);
        return EvalScore.of(value, threshold, reason).withMetricName(name);
    }

    private EvalScore evaluateAllPass(List<EvalScore> scores) {
        double minValue = scores.stream().mapToDouble(EvalScore::value).min().orElse(0.0);
        boolean allPassed = scores.stream().allMatch(EvalScore::passed);
        String reason = buildReason(scores);
        return new EvalScore(minValue, threshold, allPassed && minValue >= threshold, reason, name);
    }

    private EvalScore evaluateAnyPass(List<EvalScore> scores) {
        double maxValue = scores.stream().mapToDouble(EvalScore::value).max().orElse(0.0);
        boolean anyPassed = scores.stream().anyMatch(EvalScore::passed);
        String reason = buildReason(scores);
        return new EvalScore(maxValue, threshold, anyPassed || maxValue >= threshold, reason, name);
    }

    private String buildReason(List<EvalScore> scores) {
        var joiner = new StringJoiner("; ", "Composite [" + strategy + "]: ", "");
        for (int i = 0; i < scores.size(); i++) {
            var wm = metrics.get(i);
            var score = scores.get(i);
            joiner.add(wm.metric().name() + "=" + String.format("%.2f", score.value()));
        }
        return joiner.toString();
    }

    @Override
    public String name() {
        return name;
    }

    public List<WeightedMetric> metrics() { return metrics; }
    public CompositeStrategy strategy() { return strategy; }
    public double threshold() { return threshold; }

    public static Builder builder() {
        return new Builder();
    }

    public record WeightedMetric(EvalMetric metric, double weight) {
        public WeightedMetric {
            Objects.requireNonNull(metric, "metric must not be null");
            if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
        }
    }

    public static final class Builder {
        private String name;
        private final List<WeightedMetric> metrics = new ArrayList<>();
        private CompositeStrategy strategy = CompositeStrategy.WEIGHTED_AVERAGE;
        private double threshold = 0.5;

        private Builder() {}

        public Builder name(String name) { this.name = name; return this; }

        public Builder add(EvalMetric metric, double weight) {
            metrics.add(new WeightedMetric(metric, weight));
            return this;
        }

        public Builder strategy(CompositeStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy);
            return this;
        }

        public Builder threshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public CompositeMetric build() {
            return new CompositeMetric(this);
        }
    }
}
