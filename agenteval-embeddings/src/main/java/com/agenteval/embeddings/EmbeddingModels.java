package com.agenteval.embeddings;

import com.agenteval.core.embedding.EmbeddingModel;
import com.agenteval.embeddings.config.EmbeddingConfig;
import com.agenteval.embeddings.provider.OllamaEmbeddingModel;
import com.agenteval.embeddings.provider.OpenAiEmbeddingModel;

/**
 * Static factory for creating embedding model instances.
 *
 * <pre>{@code
 * var model = EmbeddingModels.openai("text-embedding-3-small");
 * var model = EmbeddingModels.ollama("nomic-embed-text");
 * }</pre>
 */
public final class EmbeddingModels {

    private static final String OPENAI_API_KEY_ENV = "OPENAI_API_KEY";
    private static final String OPENAI_BASE_URL = "https://api.openai.com";
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";

    private EmbeddingModels() {}

    /**
     * Creates an OpenAI embedding model using the given model ID.
     * API key is resolved from the {@code OPENAI_API_KEY} environment variable.
     */
    public static EmbeddingModel openai(String model) {
        String apiKey = System.getenv(OPENAI_API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmbeddingException(
                    "OpenAI API key not found. Set the " + OPENAI_API_KEY_ENV
                            + " environment variable or use EmbeddingModels.openai(EmbeddingConfig)");
        }
        return openai(EmbeddingConfig.builder()
                .apiKey(apiKey)
                .model(model)
                .baseUrl(OPENAI_BASE_URL)
                .build());
    }

    /**
     * Creates an OpenAI embedding model with full configuration.
     */
    public static EmbeddingModel openai(EmbeddingConfig config) {
        return new OpenAiEmbeddingModel(config);
    }

    /**
     * Creates an Ollama embedding model using the given model ID.
     * Defaults to {@code localhost:11434}.
     */
    public static EmbeddingModel ollama(String model) {
        return ollama(EmbeddingConfig.builder()
                .model(model)
                .baseUrl(OLLAMA_BASE_URL)
                .build());
    }

    /**
     * Creates an Ollama embedding model with full configuration.
     */
    public static EmbeddingModel ollama(EmbeddingConfig config) {
        return new OllamaEmbeddingModel(config);
    }
}
