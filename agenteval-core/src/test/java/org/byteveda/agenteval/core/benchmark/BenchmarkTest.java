package org.byteveda.agenteval.core.benchmark;

import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BenchmarkTest {

    private static final EvalMetric PASS_METRIC = new EvalMetric() {
        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(0.9, 0.7, "pass");
        }

        @Override
        public String name() { return "PassMetric"; }
    };

    private static final EvalMetric FAIL_METRIC = new EvalMetric() {
        @Override
        public EvalScore evaluate(AgentTestCase testCase) {
            return EvalScore.of(0.3, 0.7, "fail");
        }

        @Override
        public String name() { return "FailMetric"; }
    };

    @Test
    void sequentialRun() {
        List<AgentTestCase> cases = List.of(
                AgentTestCase.builder().input("q1").actualOutput("a1").build());

        var v1 = BenchmarkVariant.builder().name("high").metrics(List.of(PASS_METRIC)).build();
        var v2 = BenchmarkVariant.builder().name("low").metrics(List.of(FAIL_METRIC)).build();

        BenchmarkResult result = Benchmark.run(cases, List.of(v1, v2));

        assertThat(result.variantResults()).hasSize(2);
        assertThat(result.bestVariant()).isEqualTo("high");
        assertThat(result.worstVariant()).isEqualTo("low");
    }

    @Test
    void isolatedMutationsBetweenVariants() {
        AgentTestCase original = AgentTestCase.builder()
                .input("q1").actualOutput("original").build();

        var mutator = BenchmarkVariant.builder()
                .name("mutator")
                .metrics(List.of(PASS_METRIC))
                .casePreparer(tc -> {
                    tc.setActualOutput("mutated");
                    return tc;
                })
                .build();

        var observer = BenchmarkVariant.builder()
                .name("observer")
                .metrics(List.of(new EvalMetric() {
                    @Override
                    public EvalScore evaluate(AgentTestCase testCase) {
                        // Should see original, not mutated
                        return testCase.getActualOutput().equals("original")
                                ? EvalScore.pass("isolated")
                                : EvalScore.fail("leaked");
                    }

                    @Override
                    public String name() { return "IsolationCheck"; }
                }))
                .build();

        BenchmarkResult result = Benchmark.run(
                List.of(original), List.of(mutator, observer));

        // Observer should see original output due to deep copy
        EvalResult observerResult = result.resultFor("observer");
        assertThat(observerResult.passRate()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void duplicateNamesRejected() {
        var v1 = BenchmarkVariant.builder().name("same").metrics(List.of(PASS_METRIC)).build();
        var v2 = BenchmarkVariant.builder().name("same").metrics(List.of(PASS_METRIC)).build();

        List<AgentTestCase> cases = List.of(
                AgentTestCase.builder().input("q").build());

        assertThatThrownBy(() -> Benchmark.run(cases, List.of(v1, v2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void durationTracked() {
        List<AgentTestCase> cases = List.of(
                AgentTestCase.builder().input("q").actualOutput("a").build());
        var v = BenchmarkVariant.builder().name("v").metrics(List.of(PASS_METRIC)).build();

        BenchmarkResult result = Benchmark.run(cases, List.of(v));
        assertThat(result.totalDurationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void casePreparerApplied() {
        AtomicInteger counter = new AtomicInteger();

        var variant = BenchmarkVariant.builder()
                .name("counted")
                .metrics(List.of(PASS_METRIC))
                .casePreparer(tc -> {
                    counter.incrementAndGet();
                    return tc;
                })
                .build();

        List<AgentTestCase> cases = List.of(
                AgentTestCase.builder().input("q1").actualOutput("a").build(),
                AgentTestCase.builder().input("q2").actualOutput("a").build());

        Benchmark.run(cases, List.of(variant));
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void parallelMode() {
        List<AgentTestCase> cases = List.of(
                AgentTestCase.builder().input("q").actualOutput("a").build());
        var v1 = BenchmarkVariant.builder().name("p1").metrics(List.of(PASS_METRIC)).build();
        var v2 = BenchmarkVariant.builder().name("p2").metrics(List.of(PASS_METRIC)).build();

        BenchmarkConfig config = BenchmarkConfig.builder()
                .parallelVariants(true)
                .maxParallelVariants(2)
                .build();

        BenchmarkResult result = Benchmark.run(cases, List.of(v1, v2), config);
        assertThat(result.variantResults()).hasSize(2);
    }
}
