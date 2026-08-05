package org.byteveda.agenteval.junit5.extension;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricFactoryTest {

    // --- Test metric implementations with various constructors ---

    public static final class NoArgMetric implements EvalMetric {
        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(1.0, 0.5, "no-arg");
        }

        @Override
        public String name() { return "NoArg"; }
    }

    public static final class ThresholdMetric implements EvalMetric {
        private final double threshold;

        ThresholdMetric(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(0.8, threshold, "threshold");
        }

        @Override
        public String name() { return "Threshold"; }
    }

    public static final class JudgeMetric implements EvalMetric {
        private final JudgeModel judge;

        JudgeMetric(JudgeModel judge) {
            this.judge = judge;
        }

        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(0.9, 0.5, "judge: " + judge.modelId());
        }

        @Override
        public String name() { return "Judge"; }
    }

    public static final class FullMetric implements EvalMetric {
        private final JudgeModel judge;
        private final double threshold;

        FullMetric(JudgeModel judge, double threshold) {
            this.judge = judge;
            this.threshold = threshold;
        }

        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(0.7, threshold, "full: " + judge.modelId());
        }

        @Override
        public String name() { return "Full"; }
    }

    private static final JudgeModel MOCK_JUDGE = new JudgeModel() {
        @Override
        public JudgeResponse judge(String prompt) { return null; }

        @Override
        public String modelId() { return "test-model"; }
    };

    @Test
    void shouldCreateNoArgMetric() {
        EvalMetric metric = MetricFactory.create(NoArgMetric.class, -1.0, null);
        assertThat(metric).isInstanceOf(NoArgMetric.class);
        assertThat(metric.name()).isEqualTo("NoArg");
    }

    @Test
    void shouldCreateThresholdMetric() {
        EvalMetric metric = MetricFactory.create(ThresholdMetric.class, 0.8, null);
        assertThat(metric).isInstanceOf(ThresholdMetric.class);
    }

    @Test
    void shouldCreateJudgeMetric() {
        EvalMetric metric = MetricFactory.create(JudgeMetric.class, -1.0, MOCK_JUDGE);
        assertThat(metric).isInstanceOf(JudgeMetric.class);
    }

    @Test
    void shouldCreateFullMetric() {
        EvalMetric metric = MetricFactory.create(FullMetric.class, 0.7, MOCK_JUDGE);
        assertThat(metric).isInstanceOf(FullMetric.class);
    }

    @Test
    void shouldFallbackToNoArgWhenDefaultThreshold() {
        EvalMetric metric = MetricFactory.create(NoArgMetric.class, -1.0, MOCK_JUDGE);
        assertThat(metric).isInstanceOf(NoArgMetric.class);
    }

    @Test
    void shouldThrowWhenNoSuitableConstructor() {
        assertThatThrownBy(() -> MetricFactory.create(NoConstructorMetric.class, 0.5, null))
                .isInstanceOf(MetricFactory.MetricInstantiationException.class)
                .hasMessageContaining("No suitable constructor");
    }

    public static final class NoConstructorMetric implements EvalMetric {
        NoConstructorMetric(String unsupported) {}

        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.pass("never");
        }

        @Override
        public String name() { return "NoConstructor"; }
    }
}
