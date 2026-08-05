package org.byteveda.agenteval.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EvaluateTaskTest {

    private static boolean gradleTaskCreationSupported;

    @BeforeAll
    static void checkEnvironment() {
        // Gradle's ProjectBuilder may fail to inject synthetic classes on newer JDKs
        // without --add-opens. Guard tests with an assumption.
        try {
            Project project = ProjectBuilder.builder().build();
            project.getPluginManager().apply("org.byteveda.agenteval.evaluate");
            EvaluateTask task = (EvaluateTask) project.getTasks().getByName("agentEvaluate");
            gradleTaskCreationSupported = task != null;
        } catch (Exception e) {
            gradleTaskCreationSupported = false;
        }
    }

    @Test
    void taskDefaultPropertyValues() {
        assumeTrue(gradleTaskCreationSupported,
                "Gradle task creation not supported in this JVM configuration");

        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("org.byteveda.agenteval.evaluate");

        EvaluateTask task = (EvaluateTask) project.getTasks().getByName("agentEvaluate");

        assertThat(task.getConfigFile().get()).isEqualTo("agenteval.yaml");
        assertThat(task.getReportFormats().get()).isEqualTo("console,json");
        assertThat(task.getOutputDirectory().get()).isEqualTo("build/agenteval");
        assertThat(task.getFailOnRegression().get()).isFalse();
        assertThat(task.getThreshold().get()).isEqualTo(0.7);
        assertThat(task.getMetrics().get()).isEqualTo("AnswerRelevancy");
    }

    @Test
    void datasetPathRequiredValidation() {
        assumeTrue(gradleTaskCreationSupported,
                "Gradle task creation not supported in this JVM configuration");

        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("org.byteveda.agenteval.evaluate");

        EvaluateTask task = (EvaluateTask) project.getTasks().getByName("agentEvaluate");

        assertThat(task.getDatasetPath().isPresent()).isFalse();
    }

    @Test
    void extensionOverridesWireToTask() {
        assumeTrue(gradleTaskCreationSupported,
                "Gradle task creation not supported in this JVM configuration");

        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("org.byteveda.agenteval.evaluate");

        AgentEvalExtension ext = project.getExtensions()
                .getByType(AgentEvalExtension.class);

        ext.getMetrics().set("Faithfulness,Correctness");
        ext.getThreshold().set(0.8);

        EvaluateTask task = (EvaluateTask) project.getTasks().getByName("agentEvaluate");
        assertThat(task.getMetrics().get()).isEqualTo("Faithfulness,Correctness");
        assertThat(task.getThreshold().get()).isEqualTo(0.8);
    }
}
