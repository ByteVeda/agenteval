package org.byteveda.agenteval.metrics.conversation;

import org.byteveda.agenteval.core.embedding.EmbeddingModel;
import org.byteveda.agenteval.core.metric.ConversationMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ConversationTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.metrics.util.VectorMath;

import java.util.List;
import java.util.Objects;

/**
 * Embedding-based conversation metric that detects topic drift across turns.
 *
 * <p>Embeds the first turn's input as a topic anchor, then measures how well
 * subsequent turns stay on topic. Higher score = less drift (better coherence).</p>
 */
public final class TopicDriftDetectionMetric implements ConversationMetric {

    private static final String NAME = "TopicDriftDetection";
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final EmbeddingModel embeddingModel;
    private final double threshold;

    public TopicDriftDetectionMetric(EmbeddingModel embeddingModel) {
        this(embeddingModel, DEFAULT_THRESHOLD);
    }

    public TopicDriftDetectionMetric(EmbeddingModel embeddingModel, double threshold) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel,
                "embeddingModel must not be null");
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        this.threshold = threshold;
    }

    @Override
    public EvalScore evaluate(ConversationTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        List<AgentTestCase> turns = testCase.getTurns();
        if (turns.size() < 2) {
            return EvalScore.of(1.0, threshold,
                    "Single-turn conversation has no drift");
        }

        List<Double> anchorEmb = embeddingModel.embed(turns.getFirst().getInput());

        double totalSimilarity = 0.0;
        int comparisons = turns.size() - 1;

        for (int i = 1; i < turns.size(); i++) {
            String turnText = turns.get(i).getInput();
            if (turns.get(i).getActualOutput() != null) {
                turnText = turnText + " " + turns.get(i).getActualOutput();
            }
            List<Double> turnEmb = embeddingModel.embed(turnText);
            double sim = VectorMath.cosineSimilarity(anchorEmb, turnEmb);
            totalSimilarity += Math.max(0.0, sim);
        }

        double avgSimilarity = totalSimilarity / comparisons;
        double score = Math.min(1.0, Math.max(0.0, avgSimilarity));

        String reason = String.format(
                "Average topic similarity: %.4f across %d turns (model: %s)",
                avgSimilarity, comparisons, embeddingModel.modelId());

        return EvalScore.of(score, threshold, reason);
    }

    @Override
    public String name() {
        return NAME;
    }
}
