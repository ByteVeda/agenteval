package com.agenteval.core.config;

import com.agenteval.core.embedding.EmbeddingModel;
import com.agenteval.core.judge.JudgeModel;

/**
 * Configuration for the AgentEval evaluation engine.
 *
 * <pre>{@code
 * var config = AgentEvalConfig.builder()
 *     .judgeModel(myJudgeModel)
 *     .maxConcurrentJudgeCalls(4)
 *     .retryOnRateLimit(true)
 *     .maxRetries(3)
 *     .cacheJudgeResults(true)
 *     .build();
 * }</pre>
 */
public final class AgentEvalConfig {

    private final JudgeModel judgeModel;
    private final EmbeddingModel embeddingModel;
    private final int maxConcurrentJudgeCalls;
    private final boolean retryOnRateLimit;
    private final int maxRetries;
    private final boolean cacheJudgeResults;

    private AgentEvalConfig(Builder builder) {
        this.judgeModel = builder.judgeModel;
        this.embeddingModel = builder.embeddingModel;
        this.maxConcurrentJudgeCalls = builder.maxConcurrentJudgeCalls;
        this.retryOnRateLimit = builder.retryOnRateLimit;
        this.maxRetries = builder.maxRetries;
        this.cacheJudgeResults = builder.cacheJudgeResults;
    }

    public JudgeModel judgeModel() { return judgeModel; }
    public EmbeddingModel embeddingModel() { return embeddingModel; }
    public int maxConcurrentJudgeCalls() { return maxConcurrentJudgeCalls; }
    public boolean retryOnRateLimit() { return retryOnRateLimit; }
    public int maxRetries() { return maxRetries; }
    public boolean cacheJudgeResults() { return cacheJudgeResults; }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a default configuration (no judge or embedding model configured).
     */
    public static AgentEvalConfig defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private JudgeModel judgeModel;
        private EmbeddingModel embeddingModel;
        private int maxConcurrentJudgeCalls = Runtime.getRuntime().availableProcessors();
        private boolean retryOnRateLimit = true;
        private int maxRetries = 3;
        private boolean cacheJudgeResults = false;

        private Builder() {}

        public Builder judgeModel(JudgeModel judgeModel) { this.judgeModel = judgeModel; return this; }
        public Builder embeddingModel(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }
        public Builder maxConcurrentJudgeCalls(int max) {
            if (max < 1) throw new IllegalArgumentException("maxConcurrentJudgeCalls must be >= 1");
            this.maxConcurrentJudgeCalls = max;
            return this;
        }
        public Builder retryOnRateLimit(boolean retry) { this.retryOnRateLimit = retry; return this; }
        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) throw new IllegalArgumentException("maxRetries must be >= 0");
            this.maxRetries = maxRetries;
            return this;
        }
        public Builder cacheJudgeResults(boolean cache) { this.cacheJudgeResults = cache; return this; }

        public AgentEvalConfig build() {
            return new AgentEvalConfig(this);
        }
    }
}
