package org.byteveda.agenteval.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluateTaskTest {

    @Test
    void taskDefaultPropertyValues() {
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
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("org.byteveda.agenteval.evaluate");

        EvaluateTask task = (EvaluateTask) project.getTasks().getByName("agentEvaluate");

        assertThat(task.getDatasetPath().isPresent()).isFalse();
    }

    @Test
    void extensionOverridesWireToTask() {
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
