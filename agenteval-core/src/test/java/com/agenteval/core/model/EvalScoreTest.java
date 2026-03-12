package com.agenteval.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvalScoreTest {

    @Test
    void ofShouldAutoDerivePassed() {
        var passing = EvalScore.of(0.8, 0.7, "good");
        assertThat(passing.passed()).isTrue();
        assertThat(passing.value()).isEqualTo(0.8);
        assertThat(passing.threshold()).isEqualTo(0.7);
        assertThat(passing.reason()).isEqualTo("good");
        assertThat(passing.metricName()).isEmpty();

        var failing = EvalScore.of(0.5, 0.7, "below threshold");
        assertThat(failing.passed()).isFalse();
    }

    @Test
    void ofShouldPassWhenValueEqualsThreshold() {
        var score = EvalScore.of(0.7, 0.7, "exact match");
        assertThat(score.passed()).isTrue();
    }

    @Test
    void passShouldCreatePerfectScore() {
        var score = EvalScore.pass("all good");
        assertThat(score.value()).isEqualTo(1.0);
        assertThat(score.passed()).isTrue();
        assertThat(score.reason()).isEqualTo("all good");
    }

    @Test
    void failShouldCreateZeroScore() {
        var score = EvalScore.fail("bad output");
        assertThat(score.value()).isEqualTo(0.0);
        assertThat(score.passed()).isFalse();
        assertThat(score.reason()).isEqualTo("bad output");
    }

    @Test
    void withMetricNameShouldReturnNewInstance() {
        var score = EvalScore.of(0.8, 0.7, "test");
        var named = score.withMetricName("Faithfulness");

        assertThat(named.metricName()).isEqualTo("Faithfulness");
        assertThat(named.value()).isEqualTo(score.value());
        assertThat(score.metricName()).isEmpty(); // original unchanged
    }

    @Test
    void shouldRejectValueBelowZero() {
        assertThatThrownBy(() -> EvalScore.of(-0.1, 0.5, "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectValueAboveOne() {
        assertThatThrownBy(() -> EvalScore.of(1.1, 0.5, "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectThresholdBelowZero() {
        assertThatThrownBy(() -> EvalScore.of(0.5, -0.1, "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectThresholdAboveOne() {
        assertThatThrownBy(() -> EvalScore.of(0.5, 1.1, "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullReason() {
        assertThatThrownBy(() -> EvalScore.of(0.5, 0.5, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldDefaultNullMetricNameToEmpty() {
        var score = new EvalScore(0.5, 0.5, true, "test", null);
        assertThat(score.metricName()).isEmpty();
    }
}
