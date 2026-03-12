package com.agenteval.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentEvalConfigLoaderTest {

    @Test
    void shouldParseYamlConfig() {
        String yaml = """
                defaults:
                  maxRetries: 5
                  retryOnRateLimit: true
                  maxConcurrentJudgeCalls: 8
                cost:
                  budget: 10.00
                """;

        var builder = AgentEvalConfigLoader.parse(yaml);
        var config = builder.build();

        assertThat(config.maxRetries()).isEqualTo(5);
        assertThat(config.retryOnRateLimit()).isTrue();
        assertThat(config.maxConcurrentJudgeCalls()).isEqualTo(8);
        assertThat(config.costBudget()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldLoadFromPath(@TempDir Path tmpDir) throws Exception {
        Path yamlFile = tmpDir.resolve("agenteval.yaml");
        Files.writeString(yamlFile, """
                defaults:
                  maxRetries: 2
                """);

        var builder = AgentEvalConfigLoader.load(yamlFile);
        var config = builder.build();

        assertThat(config.maxRetries()).isEqualTo(2);
    }

    @Test
    void shouldResolveEnvVars() {
        String resolved = AgentEvalConfigLoader.resolveEnvVars(
                "key: ${PATH}");
        assertThat(resolved).doesNotContain("${PATH}");
    }

    @Test
    void shouldHandleMissingEnvVars() {
        String resolved = AgentEvalConfigLoader.resolveEnvVars(
                "key: ${NONEXISTENT_VAR_12345}");
        assertThat(resolved).isEqualTo("key: ");
    }

    @Test
    void shouldHandleMinimalConfig() {
        var builder = AgentEvalConfigLoader.parse("---\n");
        var config = builder.build();
        assertThat(config.maxRetries()).isEqualTo(3);
    }

    @Test
    void shouldParseJudgeSection() {
        String yaml = """
                judge:
                  provider: openai
                  model: gpt-4o
                  baseUrl: https://api.openai.com
                """;

        var builder = AgentEvalConfigLoader.parse(yaml);
        var config = builder.build();
        assertThat(config).isNotNull();
    }

    @Test
    void shouldThrowOnInvalidFile(@TempDir Path tmpDir) {
        Path missing = tmpDir.resolve("nonexistent.yaml");

        assertThatThrownBy(() -> AgentEvalConfigLoader.load(missing))
                .isInstanceOf(ConfigException.class);
    }
}
