package com.agenteval.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentEvalPluginTest {

    @Test
    void pluginAppliesSuccessfully() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("com.agenteval.evaluate");

        assertThat(project.getPlugins().hasPlugin(AgentEvalPlugin.class)).isTrue();
    }

    @Test
    void extensionRegistered() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("com.agenteval.evaluate");

        AgentEvalExtension extension = project.getExtensions()
                .findByType(AgentEvalExtension.class);
        assertThat(extension).isNotNull();
    }

    @Test
    void taskRegisteredWithCorrectTypeAndGroup() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("com.agenteval.evaluate");

        var task = project.getTasks().getByName("agentEvaluate");
        assertThat(task).isInstanceOf(EvaluateTask.class);
        assertThat(task.getGroup()).isEqualTo("verification");
    }

    @Test
    void extensionDefaultValues() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("com.agenteval.evaluate");

        AgentEvalExtension ext = project.getExtensions()
                .getByType(AgentEvalExtension.class);

        assertThat(ext.getConfigFile().get()).isEqualTo("agenteval.yaml");
        assertThat(ext.getReportFormats().get()).isEqualTo("console,json");
        assertThat(ext.getOutputDirectory().get()).isEqualTo("build/agenteval");
        assertThat(ext.getFailOnRegression().get()).isFalse();
        assertThat(ext.getThreshold().get()).isEqualTo(0.7);
        assertThat(ext.getMetrics().get()).isEqualTo("AnswerRelevancy");
    }
}
