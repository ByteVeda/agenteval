package org.byteveda.agenteval.metrics.util;

import java.util.List;

/**
 * Vector math utilities for embedding-based metrics.
 */
public final class VectorMath {

    private VectorMath() {}

    /**
     * Computes the cosine similarity between two embedding vectors.
     *
     * @param a first embedding vector
     * @param b second embedding vector
     * @return cosine similarity in range [-1.0, 1.0]
     * @throws IllegalArgumentException if vectors differ in size
     */
    public static double cosineSimilarity(List<Double> a, List<Double> b) {
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
