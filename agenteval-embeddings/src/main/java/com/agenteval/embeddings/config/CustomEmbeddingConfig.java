package com.agenteval.embeddings.config;

import java.util.Objects;

/**
 * Configuration for a custom HTTP embedding model endpoint.
 *
 * <pre>{@code
 * var customConfig = CustomEmbeddingConfig.builder()
 *     .requestTemplate("{\"text\": \"{{input}}\"}")
 *     .embeddingJsonPath("data.embedding")
 *     .authHeader("Bearer my-api-key")
 *     .build();
 * }</pre>
 */
public final class CustomEmbeddingConfig {

    private final String requestTemplate;
    private final String embeddingJsonPath;
    private final String authHeader;

    private CustomEmbeddingConfig(Builder builder) {
        this.requestTemplate = Objects.requireNonNull(builder.requestTemplate,
                "requestTemplate must not be null");
        this.embeddingJsonPath = Objects.requireNonNull(builder.embeddingJsonPath,
                "embeddingJsonPath must not be null");
        this.authHeader = builder.authHeader;
    }

    public String getRequestTemplate() { return requestTemplate; }
    public String getEmbeddingJsonPath() { return embeddingJsonPath; }
    public String getAuthHeader() { return authHeader; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String requestTemplate;
        private String embeddingJsonPath;
        private String authHeader;

        private Builder() {}

        /**
         * JSON request body template. Use {@code {{input}}} as a placeholder for the text.
         */
        public Builder requestTemplate(String requestTemplate) {
            this.requestTemplate = requestTemplate;
            return this;
        }

        /**
         * Dot-delimited JSON path to the embedding array in the response.
         * Example: {@code "data.embedding"} or {@code "result.0.values"}
         */
        public Builder embeddingJsonPath(String embeddingJsonPath) {
            this.embeddingJsonPath = embeddingJsonPath;
            return this;
        }

        /**
         * Optional Authorization header value (e.g., {@code "Bearer my-key"}).
         */
        public Builder authHeader(String authHeader) {
            this.authHeader = authHeader;
            return this;
        }

        public CustomEmbeddingConfig build() {
            return new CustomEmbeddingConfig(this);
        }
    }
}
