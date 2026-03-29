package org.byteveda.agenteval.metrics.response;

import org.byteveda.agenteval.core.embedding.EmbeddingModel;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.metrics.util.VectorMath;

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

        double similarity = VectorMath.cosineSimilarity(actualEmb, expectedEmb);
        double score = Math.min(1.0, Math.max(0.0, similarity));
        String reason = String.format("Cosine similarity: %.4f (model: %s)",
                similarity, embeddingModel.modelId());
        return EvalScore.of(score, threshold, reason);
    }

    @Override
    public String name() {
        return NAME;
    }

    /**
     * @deprecated Use {@link VectorMath#cosineSimilarity(List, List)} instead.
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    static double cosineSimilarity(List<Double> a, List<Double> b) {
        return VectorMath.cosineSimilarity(a, b);
    }
}
