package com.agenteval.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentEvalConfigTest {

    @Test
    void defaultsShouldHaveReasonableValues() {
        var config = AgentEvalConfig.defaults();

        assertThat(config.judgeModel()).isNull();
        assertThat(config.embeddingModel()).isNull();
        assertThat(config.maxConcurrentJudgeCalls()).isPositive();
        assertThat(config.retryOnRateLimit()).isTrue();
        assertThat(config.maxRetries()).isEqualTo(3);
        assertThat(config.cacheJudgeResults()).isFalse();
    }

    @Test
    void builderShouldSetAllFields() {
        var config = AgentEvalConfig.builder()
                .maxConcurrentJudgeCalls(8)
                .retryOnRateLimit(false)
                .maxRetries(5)
                .cacheJudgeResults(true)
                .build();

        assertThat(config.maxConcurrentJudgeCalls()).isEqualTo(8);
        assertThat(config.retryOnRateLimit()).isFalse();
        assertThat(config.maxRetries()).isEqualTo(5);
        assertThat(config.cacheJudgeResults()).isTrue();
    }

    @Test
    void shouldRejectZeroConcurrency() {
        assertThatThrownBy(() -> AgentEvalConfig.builder()
                .maxConcurrentJudgeCalls(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeConcurrency() {
        assertThatThrownBy(() -> AgentEvalConfig.builder()
                .maxConcurrentJudgeCalls(-1)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeMaxRetries() {
        assertThatThrownBy(() -> AgentEvalConfig.builder()
                .maxRetries(-1)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
