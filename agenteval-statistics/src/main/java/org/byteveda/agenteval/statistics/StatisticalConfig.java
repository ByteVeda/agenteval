package org.byteveda.agenteval.statistics;

import org.byteveda.agenteval.statistics.inference.ConfidenceLevel;

/**
 * Configuration for statistical analysis. Immutable; use the builder to construct.
 */
public final class StatisticalConfig {

    private final ConfidenceLevel confidenceLevel;
    private final double significanceAlpha;
    private final double cvThreshold;
    private final int bootstrapIterations;
    private final double desiredPower;

    private StatisticalConfig(Builder builder) {
        this.confidenceLevel = builder.confidenceLevel;
        this.significanceAlpha = builder.significanceAlpha;
        this.cvThreshold = builder.cvThreshold;
        this.bootstrapIterations = builder.bootstrapIterations;
        this.desiredPower = builder.desiredPower;
    }

    /**
     * Returns a new builder with default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the default configuration.
     *
     * @return default config instance
     */
    public static StatisticalConfig defaults() {
        return new Builder().build();
    }

    public ConfidenceLevel confidenceLevel() {
        return confidenceLevel;
    }

    public double significanceAlpha() {
        return significanceAlpha;
    }

    public double cvThreshold() {
        return cvThreshold;
    }

    public int bootstrapIterations() {
        return bootstrapIterations;
    }

    public double desiredPower() {
        return desiredPower;
    }

    /**
     * Builder for {@link StatisticalConfig}.
     */
    public static final class Builder {

        private ConfidenceLevel confidenceLevel = ConfidenceLevel.P95;
        private double significanceAlpha = 0.05;
        private double cvThreshold = 0.15;
        private int bootstrapIterations = 10_000;
        private double desiredPower = 0.80;

        private Builder() {
        }

        /**
         * Sets the confidence level.
         *
         * @param confidenceLevel the confidence level
         * @return this builder
         */
        public Builder confidenceLevel(ConfidenceLevel confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
            return this;
        }

        /**
         * Sets the significance alpha level.
         *
         * @param significanceAlpha the alpha level (e.g., 0.05)
         * @return this builder
         */
        public Builder significanceAlpha(double significanceAlpha) {
            this.significanceAlpha = significanceAlpha;
            return this;
        }

        /**
         * Sets the coefficient of variation threshold for high-variance flagging.
         *
         * @param cvThreshold the threshold (e.g., 0.15)
         * @return this builder
         */
        public Builder cvThreshold(double cvThreshold) {
            this.cvThreshold = cvThreshold;
            return this;
        }

        /**
         * Sets the number of bootstrap iterations.
         *
         * @param bootstrapIterations the number of iterations
         * @return this builder
         */
        public Builder bootstrapIterations(int bootstrapIterations) {
            this.bootstrapIterations = bootstrapIterations;
            return this;
        }

        /**
         * Sets the desired statistical power (1 - beta).
         *
         * @param desiredPower the power level (e.g., 0.80)
         * @return this builder
         */
        public Builder desiredPower(double desiredPower) {
            this.desiredPower = desiredPower;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return an immutable {@link StatisticalConfig}
         */
        public StatisticalConfig build() {
            return new StatisticalConfig(this);
        }
    }
}
