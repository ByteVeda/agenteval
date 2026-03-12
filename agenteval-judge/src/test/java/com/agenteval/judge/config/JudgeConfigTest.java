package com.agenteval.judge.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JudgeConfigTest {

    @Test
    void shouldBuildWithDefaults() {
        var config = JudgeConfig.builder()
                .apiKey("sk-test")
                .model("gpt-4o")
                .baseUrl("https://api.openai.com")
                .build();

        assertThat(config.getApiKey()).isEqualTo("sk-test");
        assertThat(config.getModel()).isEqualTo("gpt-4o");
        assertThat(config.getBaseUrl()).isEqualTo("https://api.openai.com");
        assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(config.getMaxRetries()).isEqualTo(3);
        assertThat(config.getTemperature()).isEqualTo(0.0);
    }

    @Test
    void shouldBuildWithCustomValues() {
        var config = JudgeConfig.builder()
                .apiKey("sk-test")
                .model("gpt-4o-mini")
                .baseUrl("https://custom.api.com")
                .timeout(Duration.ofSeconds(30))
                .maxRetries(5)
                .temperature(0.7)
                .build();

        assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getMaxRetries()).isEqualTo(5);
        assertThat(config.getTemperature()).isEqualTo(0.7);
    }

    @Test
    void shouldRejectNullApiKey() {
        assertThatThrownBy(() -> JudgeConfig.builder()
                .model("gpt-4o")
                .baseUrl("https://api.openai.com")
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullModel() {
        assertThatThrownBy(() -> JudgeConfig.builder()
                .apiKey("sk-test")
                .baseUrl("https://api.openai.com")
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNegativeMaxRetries() {
        assertThatThrownBy(() -> JudgeConfig.builder().maxRetries(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectTemperatureOutOfRange() {
        assertThatThrownBy(() -> JudgeConfig.builder().temperature(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JudgeConfig.builder().temperature(2.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
