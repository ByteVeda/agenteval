package org.byteveda.agenteval.core.benchmark;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BenchmarkVariantTest {

    private static final EvalMetric DUMMY_METRIC = new EvalMetric() {
        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.pass("ok");
        }

        @Override
        public String name() { return "Dummy"; }
    };

    @Test
    void buildWithRequiredFields() {
        BenchmarkVariant variant = BenchmarkVariant.builder()
                .name("test-variant")
                .metrics(List.of(DUMMY_METRIC))
                .build();

        assertThat(variant.name()).isEqualTo("test-variant");
        assertThat(variant.metrics()).hasSize(1);
        assertThat(variant.config()).isNotNull();
        assertThat(variant.casePreparer()).isNotNull();
    }

    @Test
    void defaultsApplied() {
        BenchmarkVariant variant = BenchmarkVariant.builder()
                .name("v1")
                .metrics(List.of(DUMMY_METRIC))
                .build();

        // Default config should be AgentEvalConfig defaults
        assertThat(variant.config().parallelEvaluation()).isFalse();
        // Default casePreparer should be identity
        AgentTestCase tc = AgentTestCase.builder().input("q").build();
        assertThat(variant.casePreparer().apply(tc)).isSameAs(tc);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> BenchmarkVariant.builder()
                .metrics(List.of(DUMMY_METRIC))
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsEmptyName() {
        assertThatThrownBy(() -> BenchmarkVariant.builder()
                .name("")
                .metrics(List.of(DUMMY_METRIC))
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyMetrics() {
        assertThatThrownBy(() -> BenchmarkVariant.builder()
                .name("v")
                .metrics(List.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullMetrics() {
        assertThatThrownBy(() -> BenchmarkVariant.builder()
                .name("v")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customCasePreparer() {
        UnaryOperator<AgentTestCase> preparer = tc ->
                tc.toBuilder().actualOutput("prepared").build();

        BenchmarkVariant variant = BenchmarkVariant.builder()
                .name("prepared")
                .metrics(List.of(DUMMY_METRIC))
                .casePreparer(preparer)
                .build();

        AgentTestCase tc = AgentTestCase.builder().input("q").build();
        AgentTestCase result = variant.casePreparer().apply(tc);
        assertThat(result.getActualOutput()).isEqualTo("prepared");
    }
}
