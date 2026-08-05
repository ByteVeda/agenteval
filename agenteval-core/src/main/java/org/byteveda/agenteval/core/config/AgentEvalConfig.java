package org.byteveda.agenteval.core.config;

import org.byteveda.agenteval.core.cost.PricingModel;
import org.byteveda.agenteval.core.embedding.EmbeddingModel;
import org.byteveda.agenteval.core.judge.CachingJudgeModel;
import org.byteveda.agenteval.core.judge.JudgeModel;

import org.byteveda.agenteval.core.eval.ProgressCallback;

import java.math.BigDecimal;

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
 *     .parallelEvaluation(true)
 *     .parallelism(8)
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
    private final BigDecimal costBudget;
    private final PricingModel pricingModel;
    private final boolean parallelEvaluation;
    private final int parallelism;
    private final ProgressCallback progressCallback;

    private AgentEvalConfig(Builder builder) {
        this.judgeModel = builder.judgeModel;
        this.embeddingModel = builder.embeddingModel;
        this.maxConcurrentJudgeCalls = builder.maxConcurrentJudgeCalls;
        this.retryOnRateLimit = builder.retryOnRateLimit;
        this.maxRetries = builder.maxRetries;
        this.cacheJudgeResults = builder.cacheJudgeResults;
        this.costBudget = builder.costBudget;
        this.pricingModel = builder.pricingModel;
        this.parallelEvaluation = builder.parallelEvaluation;
        this.parallelism = builder.parallelism;
        this.progressCallback = builder.progressCallback;
    }

    public JudgeModel judgeModel() { return judgeModel; }
    public EmbeddingModel embeddingModel() { return embeddingModel; }
    public int maxConcurrentJudgeCalls() { return maxConcurrentJudgeCalls; }
    public boolean retryOnRateLimit() { return retryOnRateLimit; }
    public int maxRetries() { return maxRetries; }
    public boolean cacheJudgeResults() { return cacheJudgeResults; }
    public BigDecimal costBudget() { return costBudget; }
    public PricingModel pricingModel() { return pricingModel; }
    public boolean parallelEvaluation() { return parallelEvaluation; }
    public int parallelism() { return parallelism; }
    public ProgressCallback progressCallback() { return progressCallback; }

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
        private BigDecimal costBudget;
        private PricingModel pricingModel;
        private boolean parallelEvaluation = false;
        private int parallelism = Runtime.getRuntime().availableProcessors();
        private ProgressCallback progressCallback;

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
        public Builder costBudget(BigDecimal budget) { this.costBudget = budget; return this; }
        public Builder pricingModel(PricingModel pricing) { this.pricingModel = pricing; return this; }
        public Builder parallelEvaluation(boolean parallel) {
            this.parallelEvaluation = parallel;
            return this;
        }
        public Builder parallelism(int parallelism) {
            if (parallelism < 1) throw new IllegalArgumentException("parallelism must be >= 1");
            this.parallelism = parallelism;
            return this;
        }
        public Builder progressCallback(ProgressCallback callback) {
            this.progressCallback = callback;
            return this;
        }

        public AgentEvalConfig build() {
            if (cacheJudgeResults && judgeModel != null
                    && !(judgeModel instanceof CachingJudgeModel)) {
                judgeModel = new CachingJudgeModel(judgeModel);
            }
            return new AgentEvalConfig(this);
        }
    }
}
