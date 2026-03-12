package com.agenteval.core.config;

import java.math.BigDecimal;

/**
 * POJO representing the {@code agenteval.yaml} configuration file structure.
 *
 * <p>Used for Jackson YAML deserialization. Maps to {@link AgentEvalConfig}
 * via {@link AgentEvalConfigLoader}.</p>
 */
public final class YamlConfigModel {

    private JudgeSection judge;
    private EmbeddingSection embedding;
    private DefaultsSection defaults;
    private CostSection cost;

    public JudgeSection getJudge() { return judge; }
    public void setJudge(JudgeSection judge) { this.judge = judge; }
    public EmbeddingSection getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingSection embedding) { this.embedding = embedding; }
    public DefaultsSection getDefaults() { return defaults; }
    public void setDefaults(DefaultsSection defaults) { this.defaults = defaults; }
    public CostSection getCost() { return cost; }
    public void setCost(CostSection cost) { this.cost = cost; }

    public static final class JudgeSection {
        private String provider;
        private String model;
        private String apiKey;
        private String baseUrl;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static final class EmbeddingSection {
        private String provider;
        private String model;
        private String apiKey;
        private String baseUrl;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static final class DefaultsSection {
        private Double threshold;
        private Integer maxRetries;
        private Boolean retryOnRateLimit;
        private Integer maxConcurrentJudgeCalls;
        private Boolean parallelEvaluation;
        private Integer parallelism;

        public Double getThreshold() { return threshold; }
        public void setThreshold(Double threshold) { this.threshold = threshold; }
        public Integer getMaxRetries() { return maxRetries; }
        public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
        public Boolean getRetryOnRateLimit() { return retryOnRateLimit; }
        public void setRetryOnRateLimit(Boolean retryOnRateLimit) {
            this.retryOnRateLimit = retryOnRateLimit;
        }
        public Integer getMaxConcurrentJudgeCalls() { return maxConcurrentJudgeCalls; }
        public void setMaxConcurrentJudgeCalls(Integer max) { this.maxConcurrentJudgeCalls = max; }
        public Boolean getParallelEvaluation() { return parallelEvaluation; }
        public void setParallelEvaluation(Boolean parallelEvaluation) {
            this.parallelEvaluation = parallelEvaluation;
        }
        public Integer getParallelism() { return parallelism; }
        public void setParallelism(Integer parallelism) { this.parallelism = parallelism; }
    }

    public static final class CostSection {
        private BigDecimal budget;

        public BigDecimal getBudget() { return budget; }
        public void setBudget(BigDecimal budget) { this.budget = budget; }
    }
}
