package com.agenteval.metrics.agent;

import com.agenteval.core.embedding.EmbeddingModel;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import com.agenteval.core.model.ToolCall;
import com.agenteval.metrics.util.VectorMath;

import java.util.List;
import java.util.Objects;

/**
 * Embedding-based metric that measures how well the agent utilized tool call results
 * in its final output.
 *
 * <p>Computes the average cosine similarity between each tool call result and the
 * actual output. Higher scores indicate the agent incorporated tool results into
 * its response.</p>
 */
public final class ToolResultUtilizationMetric implements EvalMetric {

    private static final String NAME = "ToolResultUtilization";
    private static final double DEFAULT_THRESHOLD = 0.7;

    private final EmbeddingModel embeddingModel;
    private final double threshold;

    public ToolResultUtilizationMetric(EmbeddingModel embeddingModel) {
        this(embeddingModel, DEFAULT_THRESHOLD);
    }

    public ToolResultUtilizationMetric(EmbeddingModel embeddingModel, double threshold) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel,
                "embeddingModel must not be null");
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        this.threshold = threshold;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        if (testCase.getActualOutput() == null || testCase.getActualOutput().isBlank()) {
            throw new IllegalArgumentException(NAME + " requires non-empty actualOutput");
        }

        List<ToolCall> toolCalls = testCase.getToolCalls();
        List<ToolCall> withResults = toolCalls.stream()
                .filter(tc -> tc.result() != null && !tc.result().isBlank())
                .toList();

        if (withResults.isEmpty()) {
            return EvalScore.of(1.0, threshold,
                    "No tool results to measure utilization against");
        }

        List<Double> outputEmb = embeddingModel.embed(testCase.getActualOutput());

        double totalSimilarity = 0.0;
        for (ToolCall tc : withResults) {
            List<Double> resultEmb = embeddingModel.embed(tc.result());
            double sim = VectorMath.cosineSimilarity(outputEmb, resultEmb);
            totalSimilarity += Math.max(0.0, sim);
        }

        double avgSimilarity = totalSimilarity / withResults.size();
        double score = Math.min(1.0, Math.max(0.0, avgSimilarity));

        String reason = String.format(
                "Average tool result utilization: %.4f across %d tool results (model: %s)",
                avgSimilarity, withResults.size(), embeddingModel.modelId());

        return EvalScore.of(score, threshold, reason);
    }

    @Override
    public String name() {
        return NAME;
    }
}
