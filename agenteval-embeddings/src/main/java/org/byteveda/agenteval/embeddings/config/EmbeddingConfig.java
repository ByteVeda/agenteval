package org.byteveda.agenteval.embeddings.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for an embedding model provider.
 */
public final class EmbeddingConfig {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;

    private EmbeddingConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.model = Objects.requireNonNull(builder.model, "model must not be null");
        this.baseUrl = Objects.requireNonNull(builder.baseUrl, "baseUrl must not be null");
        this.timeout = builder.timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public String getBaseUrl() { return baseUrl; }
    public Duration getTimeout() { return timeout; }

    public static final class Builder {
        private String apiKey;
        private String model;
        private String baseUrl;
        private Duration timeout = Duration.ofSeconds(30);

        private Builder() {}

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }

        public EmbeddingConfig build() {
            return new EmbeddingConfig(this);
        }
    }
}
