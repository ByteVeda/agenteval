package com.agenteval.junit5.extension;

import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import com.agenteval.junit5.annotation.AgentTest;
import com.agenteval.junit5.annotation.Metric;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentEvalExtensionTest {

    /**
     * A simple deterministic metric for testing the extension lifecycle.
     */
    public static final class AlwaysPassMetric implements EvalMetric {
        AlwaysPassMetric() {}

        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(1.0, 0.5, "always passes");
        }

        @Override
        public String name() { return "AlwaysPass"; }
    }

    public static final class AlwaysFailMetric implements EvalMetric {
        AlwaysFailMetric() {}

        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(0.0, 0.5, "always fails");
        }

        @Override
        public String name() { return "AlwaysFail"; }
    }

    @Nested
    class ParameterResolution {

        @AgentTest
        void shouldResolveAgentTestCase(AgentTestCase testCase) {
            assertThat(testCase).isNotNull();
            testCase.setActualOutput("resolved via extension");
            assertThat(testCase.getActualOutput()).isEqualTo("resolved via extension");
        }
    }

    @Nested
    class MetricEvaluation {

        @AgentTest
        @Metric(value = AlwaysPassMetric.class)
        void shouldPassWithPassingMetric(AgentTestCase testCase) {
            testCase.setActualOutput("some output");
        }
    }

    @Test
    void shouldCreateExtensionWithConfig() {
        var ext = AgentEvalExtension.withConfig(
                com.agenteval.core.config.AgentEvalConfig.defaults());
        assertThat(ext).isNotNull();
    }
}
