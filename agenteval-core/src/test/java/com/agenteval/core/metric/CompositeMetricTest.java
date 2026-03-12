package com.agenteval.core.metric;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeMetricTest {

    private static EvalMetric stubMetric(String name, double score, double threshold) {
        return new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(score, threshold, name + " result");
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    @Test
    void weightedAverageShouldCalculateCorrectly() {
        var composite = CompositeMetric.builder()
                .name("Quality")
                .add(stubMetric("A", 0.8, 0.5), 0.6)
                .add(stubMetric("B", 0.6, 0.5), 0.4)
                .strategy(CompositeStrategy.WEIGHTED_AVERAGE)
                .threshold(0.7)
                .build();

        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();
        var score = composite.evaluate(tc);

        // (0.8 * 0.6 + 0.6 * 0.4) / (0.6 + 0.4) = 0.72
        assertThat(score.value()).isCloseTo(0.72, org.assertj.core.data.Offset.offset(0.001));
        assertThat(score.passed()).isTrue();
        assertThat(score.metricName()).isEqualTo("Quality");
    }

    @Test
    void weightedAverageShouldFailBelowThreshold() {
        var composite = CompositeMetric.builder()
                .name("Quality")
                .add(stubMetric("A", 0.4, 0.5), 0.5)
                .add(stubMetric("B", 0.6, 0.5), 0.5)
                .strategy(CompositeStrategy.WEIGHTED_AVERAGE)
                .threshold(0.7)
                .build();

        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();
        var score = composite.evaluate(tc);

        // (0.4 * 0.5 + 0.6 * 0.5) / 1.0 = 0.5
        assertThat(score.value()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void allPassShouldRequireAllMetricsToPass() {
        var composite = CompositeMetric.builder()
                .name("AllPass")
                .add(stubMetric("A", 0.9, 0.7), 1.0)
                .add(stubMetric("B", 0.5, 0.7), 1.0) // fails its own threshold
                .strategy(CompositeStrategy.ALL_PASS)
                .threshold(0.3)
                .build();

        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();
        var score = composite.evaluate(tc);

        assertThat(score.value()).isEqualTo(0.5); // min value
        assertThat(score.passed()).isFalse(); // B didn't pass its threshold
    }

    @Test
    void allPassShouldPassWhenAllMetricsPass() {
        var composite = CompositeMetric.builder()
                .name("AllPass")
                .add(stubMetric("A", 0.9, 0.7), 1.0)
                .add(stubMetric("B", 0.8, 0.7), 1.0)
                .strategy(CompositeStrategy.ALL_PASS)
                .threshold(0.5)
                .build();

        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();
        var score = composite.evaluate(tc);

        assertThat(score.value()).isEqualTo(0.8);
        assertThat(score.passed()).isTrue();
    }

    @Test
    void anyPassShouldPassIfAnyMetricPasses() {
        var composite = CompositeMetric.builder()
                .name("AnyPass")
                .add(stubMetric("A", 0.3, 0.7), 1.0) // fails
                .add(stubMetric("B", 0.9, 0.7), 1.0) // passes
                .strategy(CompositeStrategy.ANY_PASS)
                .threshold(0.5)
                .build();

        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();
        var score = composite.evaluate(tc);

        assertThat(score.value()).isEqualTo(0.9); // max value
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldRejectNullName() {
        assertThatThrownBy(() -> CompositeMetric.builder()
                .add(stubMetric("A", 0.5, 0.5), 1.0)
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectEmptyMetrics() {
        assertThatThrownBy(() -> CompositeMetric.builder()
                .name("Empty")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void shouldRejectNonPositiveWeight() {
        assertThatThrownBy(() -> CompositeMetric.builder()
                .name("Bad")
                .add(stubMetric("A", 0.5, 0.5), 0.0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void nameShouldReturnConfiguredName() {
        var composite = CompositeMetric.builder()
                .name("MyComposite")
                .add(stubMetric("A", 0.5, 0.5), 1.0)
                .build();

        assertThat(composite.name()).isEqualTo("MyComposite");
    }
}
