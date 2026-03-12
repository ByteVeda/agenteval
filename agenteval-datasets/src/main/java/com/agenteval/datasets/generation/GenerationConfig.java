package com.agenteval.datasets.generation;

import com.agenteval.core.judge.JudgeModel;

import java.util.Objects;

/**
 * Configuration for synthetic dataset generation.
 */
public final class GenerationConfig {

    private final JudgeModel judgeModel;
    private final int maxCasesPerDocument;
    private final String difficulty;

    private GenerationConfig(Builder builder) {
        this.judgeModel = Objects.requireNonNull(builder.judgeModel,
                "judgeModel must not be null");
        this.maxCasesPerDocument = builder.maxCasesPerDocument;
        this.difficulty = builder.difficulty;
    }

    public JudgeModel judgeModel() { return judgeModel; }
    public int maxCasesPerDocument() { return maxCasesPerDocument; }
    public String difficulty() { return difficulty; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private JudgeModel judgeModel;
        private int maxCasesPerDocument = 5;
        private String difficulty = "medium";

        private Builder() {}

        public Builder judgeModel(JudgeModel judgeModel) {
            this.judgeModel = judgeModel;
            return this;
        }

        public Builder maxCasesPerDocument(int max) {
            if (max < 1) throw new IllegalArgumentException("maxCasesPerDocument must be >= 1");
            this.maxCasesPerDocument = max;
            return this;
        }

        public Builder difficulty(String difficulty) {
            this.difficulty = Objects.requireNonNull(difficulty);
            return this;
        }

        public GenerationConfig build() {
            return new GenerationConfig(this);
        }
    }
}
