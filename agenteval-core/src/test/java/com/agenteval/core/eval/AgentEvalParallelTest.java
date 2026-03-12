package com.agenteval.core.eval;

import com.agenteval.core.config.AgentEvalConfig;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AgentEvalParallelTest {

    private static EvalMetric slowMetric(long sleepMs) {
        return new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return EvalScore.of(0.9, 0.7, "ok");
            }

            @Override
            public String name() {
                return "SlowMetric";
            }
        };
    }

    @Test
    void parallelEvaluationShouldProduceCorrectResults() {
        List<AgentTestCase> cases = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cases.add(AgentTestCase.builder()
                    .input("q" + i).actualOutput("a" + i).build());
        }

        var config = AgentEvalConfig.builder()
                .parallelEvaluation(true)
                .parallelism(4)
                .build();

        var result = AgentEval.evaluate(cases, List.of(slowMetric(10)), config);

        assertThat(result.caseResults()).hasSize(10);
        assertThat(result.passRate()).isEqualTo(1.0);
    }

    @Test
    void parallelShouldBeFasterThanSequential() {
        List<AgentTestCase> cases = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            cases.add(AgentTestCase.builder()
                    .input("q" + i).actualOutput("a" + i).build());
        }

        var seqConfig = AgentEvalConfig.builder()
                .parallelEvaluation(false)
                .build();
        long seqStart = System.currentTimeMillis();
        AgentEval.evaluate(cases, List.of(slowMetric(50)), seqConfig);
        long seqDuration = System.currentTimeMillis() - seqStart;

        var parConfig = AgentEvalConfig.builder()
                .parallelEvaluation(true)
                .parallelism(8)
                .build();
        long parStart = System.currentTimeMillis();
        AgentEval.evaluate(cases, List.of(slowMetric(50)), parConfig);
        long parDuration = System.currentTimeMillis() - parStart;

        assertThat(parDuration).isLessThan(seqDuration);
    }

    @Test
    void parallelEvaluationShouldRespectBoundedConcurrency() {
        AtomicInteger concurrent = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        EvalMetric trackingMetric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                int cur = concurrent.incrementAndGet();
                maxConcurrent.updateAndGet(prev -> Math.max(prev, cur));
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                concurrent.decrementAndGet();
                return EvalScore.of(0.9, 0.7, "ok");
            }

            @Override
            public String name() {
                return "TrackingMetric";
            }
        };

        List<AgentTestCase> cases = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            cases.add(AgentTestCase.builder()
                    .input("q" + i).actualOutput("a" + i).build());
        }

        var config = AgentEvalConfig.builder()
                .parallelEvaluation(true)
                .parallelism(3)
                .build();

        AgentEval.evaluate(cases, List.of(trackingMetric), config);

        assertThat(maxConcurrent.get()).isLessThanOrEqualTo(3);
    }

    @Test
    void progressCallbackShouldBeInvokedForEachCase() {
        List<ProgressEvent> events = Collections.synchronizedList(new ArrayList<>());

        List<AgentTestCase> cases = List.of(
                AgentTestCase.builder().input("q1").actualOutput("a1").build(),
                AgentTestCase.builder().input("q2").actualOutput("a2").build(),
                AgentTestCase.builder().input("q3").actualOutput("a3").build());

        EvalMetric metric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(1.0, 0.5, "pass");
            }

            @Override
            public String name() {
                return "SimpleMetric";
            }
        };

        var config = AgentEvalConfig.builder()
                .parallelEvaluation(false)
                .progressCallback(events::add)
                .build();

        AgentEval.evaluate(cases, List.of(metric), config);

        assertThat(events).hasSize(3);
        assertThat(events.get(0).completedCases()).isEqualTo(1);
        assertThat(events.get(1).completedCases()).isEqualTo(2);
        assertThat(events.get(2).completedCases()).isEqualTo(3);
        assertThat(events.get(2).totalCases()).isEqualTo(3);
    }

    @Test
    void parallelProgressCallbackShouldReachTotalCount() {
        List<ProgressEvent> events = Collections.synchronizedList(new ArrayList<>());

        List<AgentTestCase> cases = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cases.add(AgentTestCase.builder()
                    .input("q" + i).actualOutput("a" + i).build());
        }

        EvalMetric metric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(1.0, 0.5, "pass");
            }

            @Override
            public String name() {
                return "SimpleMetric";
            }
        };

        var config = AgentEvalConfig.builder()
                .parallelEvaluation(true)
                .parallelism(3)
                .progressCallback(events::add)
                .build();

        AgentEval.evaluate(cases, List.of(metric), config);

        assertThat(events).hasSize(5);
        int maxCompleted = events.stream()
                .mapToInt(ProgressEvent::completedCases).max().orElse(0);
        assertThat(maxCompleted).isEqualTo(5);
    }
}
