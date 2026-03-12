package com.agenteval.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenUsageTest {

    @Test
    void shouldCreateTokenUsage() {
        var usage = new TokenUsage(100, 50, 150);

        assertThat(usage.inputTokens()).isEqualTo(100);
        assertThat(usage.outputTokens()).isEqualTo(50);
        assertThat(usage.totalTokens()).isEqualTo(150);
    }

    @Test
    void ofShouldAutoCalculateTotal() {
        var usage = TokenUsage.of(100, 50);

        assertThat(usage.inputTokens()).isEqualTo(100);
        assertThat(usage.outputTokens()).isEqualTo(50);
        assertThat(usage.totalTokens()).isEqualTo(150);
    }

    @Test
    void shouldRejectNegativeInputTokens() {
        assertThatThrownBy(() -> new TokenUsage(-1, 50, 49))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeOutputTokens() {
        assertThatThrownBy(() -> new TokenUsage(100, -1, 99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeTotalTokens() {
        assertThatThrownBy(() -> new TokenUsage(100, 50, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSupportZeroValues() {
        var usage = new TokenUsage(0, 0, 0);
        assertThat(usage.totalTokens()).isZero();
    }
}
