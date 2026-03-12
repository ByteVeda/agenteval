package com.agenteval.core.embedding;

import java.util.List;

/**
 * SPI interface for embedding model providers.
 *
 * <p>Implementations are provided by {@code agenteval-embeddings} module.
 * Used by metrics like Semantic Similarity that require vector embeddings.</p>
 */
public interface EmbeddingModel {

    /**
     * Generates an embedding vector for the given text.
     *
     * @param text the input text to embed
     * @return the embedding vector as a list of doubles
     */
    List<Double> embed(String text);

    /**
     * Returns the model identifier (e.g., "text-embedding-3-small").
     */
    String modelId();
}
