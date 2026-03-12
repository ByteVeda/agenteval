package com.agenteval.judge.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for a judge LLM provider.
 */
public final class JudgeConfig {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;
    private final double temperature;

    private JudgeConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.model = Objects.requireNonNull(builder.model, "model must not be null");
        this.baseUrl = Objects.requireNonNull(builder.baseUrl, "baseUrl must not be null");
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.temperature = builder.temperature;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getApiKey() { return apiKey; }

    public String getModel() { return model; }

    public String getBaseUrl() { return baseUrl; }

    public Duration getTimeout() { return timeout; }

    public int getMaxRetries() { return maxRetries; }

    public double getTemperature() { return temperature; }

    public static final class Builder {
        private String apiKey;
        private String model;
        private String baseUrl;
        private Duration timeout = Duration.ofSeconds(60);
        private int maxRetries = 3;
        private double temperature = 0.0;

        private Builder() {}

        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }

        public Builder model(String model) { this.model = model; return this; }

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }

        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }

        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must be non-negative");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder temperature(double temperature) {
            if (temperature < 0.0 || temperature > 2.0) {
                throw new IllegalArgumentException(
                        "temperature must be between 0.0 and 2.0, got: " + temperature);
            }
            this.temperature = temperature;
            return this;
        }

        public JudgeConfig build() {
            return new JudgeConfig(this);
        }
    }
}
