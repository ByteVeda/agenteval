package com.agenteval.metrics.response;

import com.agenteval.core.embedding.EmbeddingModel;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic metric that measures semantic similarity between actual
 * and expected output using embedding cosine similarity.
 *
 * <p>Requires an {@link EmbeddingModel} to generate vector representations.
 * No LLM judge needed.</p>
 */
public final class SemanticSimilarityMetric implements EvalMetric {

    private static final String NAME = "SemanticSimilarity";
    private static final double DEFAULT_THRESHOLD = 0.7;

    private final EmbeddingModel embeddingModel;
    private final double threshold;

    public SemanticSimilarityMetric(EmbeddingModel embeddingModel) {
        this(embeddingModel, DEFAULT_THRESHOLD);
    }

    public SemanticSimilarityMetric(EmbeddingModel embeddingModel, double threshold) {
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
        if (testCase.getExpectedOutput() == null || testCase.getExpectedOutput().isBlank()) {
            throw new IllegalArgumentException(NAME + " requires non-empty expectedOutput");
        }

        List<Double> actualEmb = embeddingModel.embed(testCase.getActualOutput());
        List<Double> expectedEmb = embeddingModel.embed(testCase.getExpectedOutput());

        double similarity = cosineSimilarity(actualEmb, expectedEmb);
        double score = Math.min(1.0, Math.max(0.0, similarity));
        String reason = String.format("Cosine similarity: %.4f (model: %s)",
                similarity, embeddingModel.modelId());
        return EvalScore.of(score, threshold, reason);
    }

    @Override
    public String name() {
        return NAME;
    }

    static double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException(
                    "Embedding dimensions must match: " + a.size() + " vs " + b.size());
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }
        return dotProduct / denominator;
    }
}
