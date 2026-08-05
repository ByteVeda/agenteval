package org.byteveda.agenteval.core.benchmark;

/**
 * Configuration for benchmark execution.
 *
 * <pre>{@code
 * var config = BenchmarkConfig.builder()
 *     .parallelVariants(true)
 *     .maxParallelVariants(4)
 *     .build();
 * }</pre>
 */
public final class BenchmarkConfig {

    private final boolean parallelVariants;
    private final int maxParallelVariants;

    private BenchmarkConfig(Builder builder) {
        this.parallelVariants = builder.parallelVariants;
        this.maxParallelVariants = builder.maxParallelVariants;
    }

    public boolean parallelVariants() { return parallelVariants; }
    public int maxParallelVariants() { return maxParallelVariants; }

    public static Builder builder() {
        return new Builder();
    }

    public static BenchmarkConfig defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private boolean parallelVariants = false;
        private int maxParallelVariants = Runtime.getRuntime().availableProcessors();

        private Builder() {}

        public Builder parallelVariants(boolean parallel) {
            this.parallelVariants = parallel;
            return this;
        }

        public Builder maxParallelVariants(int max) {
            if (max < 1) {
                throw new IllegalArgumentException("maxParallelVariants must be >= 1");
            }
            this.maxParallelVariants = max;
            return this;
        }

        public BenchmarkConfig build() {
            return new BenchmarkConfig(this);
        }
    }
}
