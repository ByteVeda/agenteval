package com.agenteval.gradle;

import org.gradle.api.provider.Property;

/**
 * Extension for the AgentEval Gradle plugin.
 *
 * <p>Conventions (defaults) are set by {@link AgentEvalPlugin#apply}.</p>
 *
 * <pre>{@code
 * agenteval {
 *     datasetPath = 'src/test/resources/golden-set.json'
 *     configFile = 'agenteval.yaml'
 *     reportFormats = 'console,json'
 *     outputDirectory = 'build/agenteval'
 *     failOnRegression = false
 *     threshold = 0.7
 *     metrics = 'AnswerRelevancy'
 * }
 * }</pre>
 */
public abstract class AgentEvalExtension {

    /**
     * Path to the evaluation dataset file (required).
     */
    public abstract Property<String> getDatasetPath();

    /**
     * Path to the YAML configuration file. Default: "agenteval.yaml".
     */
    public abstract Property<String> getConfigFile();

    /**
     * Comma-separated report format names. Default: "console,json".
     */
    public abstract Property<String> getReportFormats();

    /**
     * Output directory for reports. Default: "build/agenteval".
     */
    public abstract Property<String> getOutputDirectory();

    /**
     * Whether to fail the build on regression. Default: false.
     */
    public abstract Property<Boolean> getFailOnRegression();

    /**
     * Pass/fail threshold for scores. Default: 0.7.
     */
    public abstract Property<Double> getThreshold();

    /**
     * Comma-separated metric names. Default: "AnswerRelevancy".
     */
    public abstract Property<String> getMetrics();
}
