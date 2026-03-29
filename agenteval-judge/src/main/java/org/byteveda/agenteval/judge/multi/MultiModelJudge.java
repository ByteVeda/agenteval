package org.byteveda.agenteval.judge.multi;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.TokenUsage;
import org.byteveda.agenteval.judge.JudgeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * A composite judge that fans out evaluation to multiple {@link JudgeModel} instances
 * and aggregates their scores using a configurable {@link ConsensusStrategy}.
 *
 * <p>Judges are invoked concurrently using virtual threads. The last aggregated
 * {@link MultiJudgeResponse} is available via {@link #lastMultiJudgeResponse()}.</p>
 *
 * <pre>{@code
 * var judge = MultiModelJudge.builder()
 *     .add(JudgeModels.openai("gpt-4o"))
 *     .add(JudgeModels.anthropic("claude-sonnet-4-20250514"), 2.0)
 *     .strategy(ConsensusStrategy.WEIGHTED_AVERAGE)
 *     .build();
 * }</pre>
 */
public final class MultiModelJudge implements JudgeModel {

    private static final Logger LOG = LoggerFactory.getLogger(MultiModelJudge.class);

    private final List<WeightedJudge> judges;
    private final ConsensusStrategy strategy;
    private final boolean failOnAnyError;
    private final ThreadLocal<MultiJudgeResponse> lastResponse = new ThreadLocal<>();

    private MultiModelJudge(Builder builder) {
        this.judges = List.copyOf(builder.judges);
        this.strategy = builder.strategy;
        this.failOnAnyError = builder.failOnAnyError;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public JudgeResponse judge(String prompt) {
        LOG.debug("Multi-model judge invoked with {} judges, strategy={}", judges.size(), strategy);

        List<IndividualJudgeResult> results = fanOut(prompt);
        List<IndividualJudgeResult> successful = results.stream()
                .filter(IndividualJudgeResult::succeeded)
                .toList();

        if (successful.isEmpty()) {
            String errors = results.stream()
                    .map(r -> r.modelId() + ": " + r.error())
                    .collect(Collectors.joining("; "));
            throw new JudgeException("All judges failed: " + errors);
        }

        JudgeResponse consensus = aggregate(successful);
        var multiResponse = new MultiJudgeResponse(consensus, results);
        lastResponse.set(multiResponse);

        return consensus;
    }

    @Override
    public String modelId() {
        return "multi[" + judges.stream()
                .map(wj -> wj.model().modelId())
                .collect(Collectors.joining(",")) + "]";
    }

    /**
     * Returns the full multi-judge response from the last invocation on this thread.
     */
    public MultiJudgeResponse lastMultiJudgeResponse() {
        return lastResponse.get();
    }

    private List<IndividualJudgeResult> fanOut(String prompt) {
        List<IndividualJudgeResult> results = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<IndividualJudgeResult>> futures = new ArrayList<>();

            for (WeightedJudge wj : judges) {
                futures.add(executor.submit(() -> invokeJudge(wj, prompt)));
            }

            for (Future<IndividualJudgeResult> future : futures) {
                results.add(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JudgeException("Multi-model judge interrupted", e);
        } catch (ExecutionException e) {
            throw new JudgeException("Multi-model judge execution failed", e.getCause());
        }

        if (failOnAnyError) {
            List<IndividualJudgeResult> failures = results.stream()
                    .filter(r -> !r.succeeded())
                    .toList();
            if (!failures.isEmpty()) {
                String errors = failures.stream()
                        .map(r -> r.modelId() + ": " + r.error())
                        .collect(Collectors.joining("; "));
                throw new JudgeException("Judge(s) failed with failOnAnyError=true: " + errors);
            }
        }

        return results;
    }

    @SuppressWarnings("IllegalCatch")
    private IndividualJudgeResult invokeJudge(WeightedJudge wj, String prompt) {
        try {
            JudgeResponse response = wj.model().judge(prompt);
            LOG.debug("Judge {} returned score={}", wj.model().modelId(), response.score());
            return IndividualJudgeResult.success(wj.model().modelId(), response, wj.weight());
        } catch (Exception e) {
            LOG.warn("Judge {} failed: {}", wj.model().modelId(), e.getMessage());
            return IndividualJudgeResult.failure(wj.model().modelId(), wj.weight(), e.getMessage());
        }
    }

    private JudgeResponse aggregate(List<IndividualJudgeResult> successful) {
        return switch (strategy) {
            case AVERAGE -> aggregateAverage(successful);
            case WEIGHTED_AVERAGE -> aggregateWeightedAverage(successful);
            case MAJORITY -> aggregateMajority(successful);
            case UNANIMOUS -> aggregateUnanimous(successful);
        };
    }

    private JudgeResponse aggregateAverage(List<IndividualJudgeResult> results) {
        double avgScore = results.stream()
                .mapToDouble(r -> r.response().score())
                .average()
                .orElse(0.0);
        return buildConsensusResponse(avgScore, results, "Average of " + results.size() + " judges");
    }

    private JudgeResponse aggregateWeightedAverage(List<IndividualJudgeResult> results) {
        double totalWeight = results.stream().mapToDouble(IndividualJudgeResult::weight).sum();
        double weightedSum = results.stream()
                .mapToDouble(r -> r.response().score() * r.weight())
                .sum();
        double score = totalWeight > 0 ? weightedSum / totalWeight : 0.0;
        return buildConsensusResponse(score, results,
                "Weighted average of " + results.size() + " judges");
    }

    private JudgeResponse aggregateMajority(List<IndividualJudgeResult> results) {
        // Majority: use average score, but reason reflects majority vote
        double avgScore = results.stream()
                .mapToDouble(r -> r.response().score())
                .average()
                .orElse(0.0);
        long highScoreCount = results.stream()
                .filter(r -> r.response().score() >= 0.5)
                .count();
        boolean majorityPass = highScoreCount > results.size() / 2.0;
        String reason = String.format("Majority vote: %d/%d judges scored >= 0.5",
                highScoreCount, results.size());
        double finalScore = majorityPass ? avgScore : Math.min(avgScore, 0.49);
        return buildConsensusResponse(finalScore, results, reason);
    }

    private JudgeResponse aggregateUnanimous(List<IndividualJudgeResult> results) {
        double avgScore = results.stream()
                .mapToDouble(r -> r.response().score())
                .average()
                .orElse(0.0);
        boolean allPass = results.stream()
                .allMatch(r -> r.response().score() >= 0.5);
        String reason = allPass
                ? "Unanimous: all " + results.size() + " judges scored >= 0.5"
                : "Not unanimous: not all judges scored >= 0.5";
        double finalScore = allPass ? avgScore : 0.0;
        return buildConsensusResponse(finalScore, results, reason);
    }

    private JudgeResponse buildConsensusResponse(double score,
                                                 List<IndividualJudgeResult> results,
                                                 String reason) {
        int totalInput = results.stream()
                .mapToInt(r -> r.response().tokenUsage() != null
                        ? r.response().tokenUsage().inputTokens() : 0)
                .sum();
        int totalOutput = results.stream()
                .mapToInt(r -> r.response().tokenUsage() != null
                        ? r.response().tokenUsage().outputTokens() : 0)
                .sum();
        TokenUsage totalUsage = TokenUsage.of(totalInput, totalOutput);

        return new JudgeResponse(score, reason, totalUsage);
    }

    public static final class Builder {
        private final List<WeightedJudge> judges = new ArrayList<>();
        private ConsensusStrategy strategy = ConsensusStrategy.AVERAGE;
        private boolean failOnAnyError;

        private Builder() {}

        /** Adds a judge with default weight of 1.0. */
        public Builder add(JudgeModel model) {
            return add(model, 1.0);
        }

        /** Adds a judge with the specified weight. */
        public Builder add(JudgeModel model, double weight) {
            judges.add(new WeightedJudge(model, weight));
            return this;
        }

        /** Sets the consensus strategy. Default is {@link ConsensusStrategy#AVERAGE}. */
        public Builder strategy(ConsensusStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
            return this;
        }

        /** If true, throws when any single judge fails. Default is false. */
        public Builder failOnAnyError(boolean failOnAnyError) {
            this.failOnAnyError = failOnAnyError;
            return this;
        }

        public MultiModelJudge build() {
            if (judges.isEmpty()) {
                throw new IllegalStateException("At least one judge must be added");
            }
            return new MultiModelJudge(this);
        }
    }
}
