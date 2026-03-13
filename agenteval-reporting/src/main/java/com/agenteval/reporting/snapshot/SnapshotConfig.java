package com.agenteval.reporting.snapshot;

import java.nio.file.Path;

/**
 * Configuration for snapshot testing.
 *
 * <pre>{@code
 * var config = SnapshotConfig.builder()
 *     .snapshotDirectory(Path.of("src/test/resources/agenteval-snapshots"))
 *     .updateSnapshots(false)
 *     .failOnRegression(true)
 *     .regressionThreshold(0.0)
 *     .build();
 * }</pre>
 */
public final class SnapshotConfig {

    private static final Path DEFAULT_DIRECTORY = Path.of("src/test/resources/agenteval-snapshots");

    private final Path snapshotDirectory;
    private final boolean updateSnapshots;
    private final boolean failOnRegression;
    private final double regressionThreshold;

    private SnapshotConfig(Builder builder) {
        this.snapshotDirectory = builder.snapshotDirectory;
        this.updateSnapshots = builder.updateSnapshots;
        this.failOnRegression = builder.failOnRegression;
        this.regressionThreshold = builder.regressionThreshold;
    }

    public Path snapshotDirectory() { return snapshotDirectory; }
    public boolean updateSnapshots() { return updateSnapshots; }
    public boolean failOnRegression() { return failOnRegression; }
    public double regressionThreshold() { return regressionThreshold; }

    public static Builder builder() {
        return new Builder();
    }

    public static SnapshotConfig defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private Path snapshotDirectory = DEFAULT_DIRECTORY;
        private boolean updateSnapshots = false;
        private boolean failOnRegression = true;
        private double regressionThreshold = 0.0;

        private Builder() {}

        public Builder snapshotDirectory(Path directory) {
            if (directory == null) {
                throw new IllegalArgumentException("snapshotDirectory must not be null");
            }
            this.snapshotDirectory = directory;
            return this;
        }

        public Builder updateSnapshots(boolean update) {
            this.updateSnapshots = update;
            return this;
        }

        public Builder failOnRegression(boolean fail) {
            this.failOnRegression = fail;
            return this;
        }

        public Builder regressionThreshold(double threshold) {
            if (threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException(
                        "regressionThreshold must be between 0.0 and 1.0, got: " + threshold);
            }
            this.regressionThreshold = threshold;
            return this;
        }

        public SnapshotConfig build() {
            return new SnapshotConfig(this);
        }
    }
}
