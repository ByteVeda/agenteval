package org.byteveda.agenteval.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle plugin for running AgentEval evaluations.
 *
 * <p>Registers the {@code agenteval} extension and an {@code agentEvaluate} task
 * in the {@code verification} group.</p>
 *
 * <pre>{@code
 * plugins {
 *     id 'org.byteveda.agenteval.evaluate'
 * }
 *
 * agenteval {
 *     datasetPath = 'src/test/resources/golden-set.json'
 *     metrics = 'AnswerRelevancy,Faithfulness'
 *     reportFormats = 'console,json'
 * }
 * }</pre>
 */
public class AgentEvalPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        AgentEvalExtension extension = project.getExtensions()
                .create("agenteval", AgentEvalExtension.class);

        // Set convention defaults on the extension
        extension.getConfigFile().convention("agenteval.yaml");
        extension.getReportFormats().convention("console,json");
        extension.getOutputDirectory().convention("build/agenteval");
        extension.getFailOnRegression().convention(false);
        extension.getThreshold().convention(0.7);
        extension.getMetrics().convention("AnswerRelevancy");

        project.getTasks().register("agentEvaluate", EvaluateTask.class, task -> {
            task.setGroup("verification");
            task.setDescription("Runs AgentEval evaluations against a dataset");
            task.getDatasetPath().convention(extension.getDatasetPath());
            task.getConfigFile().convention(extension.getConfigFile());
            task.getReportFormats().convention(extension.getReportFormats());
            task.getOutputDirectory().convention(extension.getOutputDirectory());
            task.getFailOnRegression().convention(extension.getFailOnRegression());
            task.getThreshold().convention(extension.getThreshold());
            task.getMetrics().convention(extension.getMetrics());
        });
    }
}
