package com.agenteval.reporting;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration for the HTML report generator.
 */
public final class HtmlReportConfig {

    private final Path outputPath;
    private final String title;
    private final boolean includeDetails;

    private HtmlReportConfig(Builder builder) {
        this.outputPath = Objects.requireNonNull(builder.outputPath,
                "outputPath must not be null");
        this.title = builder.title;
        this.includeDetails = builder.includeDetails;
    }

    public Path outputPath() { return outputPath; }
    public String title() { return title; }
    public boolean includeDetails() { return includeDetails; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Path outputPath;
        private String title = "AgentEval Report";
        private boolean includeDetails = true;

        private Builder() {}

        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder includeDetails(boolean includeDetails) {
            this.includeDetails = includeDetails;
            return this;
        }

        public HtmlReportConfig build() {
            return new HtmlReportConfig(this);
        }
    }
}
