package org.byteveda.agenteval.github;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration for the Markdown report generator.
 */
public final class MarkdownConfig {

    private final Path outputPath;
    private final boolean includeFailedDetails;
    private final boolean includeMetricBreakdown;

    private MarkdownConfig(Builder builder) {
        this.outputPath = Objects.requireNonNull(builder.outputPath,
                "outputPath must not be null");
        this.includeFailedDetails = builder.includeFailedDetails;
        this.includeMetricBreakdown = builder.includeMetricBreakdown;
    }

    public Path outputPath() { return outputPath; }
    public boolean includeFailedDetails() { return includeFailedDetails; }
    public boolean includeMetricBreakdown() { return includeMetricBreakdown; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Path outputPath;
        private boolean includeFailedDetails = true;
        private boolean includeMetricBreakdown = true;

        private Builder() {}

        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder includeFailedDetails(boolean includeFailedDetails) {
            this.includeFailedDetails = includeFailedDetails;
            return this;
        }

        public Builder includeMetricBreakdown(boolean includeMetricBreakdown) {
            this.includeMetricBreakdown = includeMetricBreakdown;
            return this;
        }

        public MarkdownConfig build() {
            return new MarkdownConfig(this);
        }
    }
}
