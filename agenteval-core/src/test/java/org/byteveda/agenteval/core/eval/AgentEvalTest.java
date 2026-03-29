package org.byteveda.agenteval.core.eval;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentEvalTest {

    private static EvalMetric passingMetric() {
        return new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.9, 0.7, "looks good");
            }

            @Override
            public String name() {
                return "PassingMetric";
            }
        };
    }

    private static EvalMetric failingMetric() {
        return new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.3, 0.7, "below threshold");
            }

            @Override
            public String name() {
                return "FailingMetric";
            }
        };
    }

    private static EvalMetric throwingMetric() {
        return new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                throw new RuntimeException("LLM provider error");
            }

            @Override
            public String name() {
                return "ThrowingMetric";
            }
        };
    }

    @Test
    void shouldEvaluateAllTestCasesAgainstAllMetrics() {
        var tc1 = AgentTestCase.builder().input("q1").actualOutput("a1").build();
        var tc2 = AgentTestCase.builder().input("q2").actualOutput("a2").build();

        var result = AgentEval.evaluate(
                List.of(tc1, tc2),
                List.of(passingMetric())
        );

        assertThat(result.caseResults()).hasSize(2);
        assertThat(result.passRate()).isEqualTo(1.0);
        assertThat(result.durationMs()).isNotNegative();
    }

    @Test
    void shouldTrackPassAndFailSeparately() {
        var tc = AgentTestCase.builder().input("q1").actualOutput("a1").build();

        var result = AgentEval.evaluate(
                List.of(tc),
                List.of(passingMetric(), failingMetric())
        );

        assertThat(result.caseResults()).hasSize(1);
        var caseResult = result.caseResults().getFirst();
        assertThat(caseResult.passed()).isFalse(); // one metric failed
        assertThat(caseResult.scores()).hasSize(2);
        assertThat(caseResult.scores().get("PassingMetric").passed()).isTrue();
        assertThat(caseResult.scores().get("FailingMetric").passed()).isFalse();
    }

    @Test
    void shouldHandleMetricExceptionsGracefully() {
        var tc = AgentTestCase.builder().input("test").actualOutput("output").build();

        var result = AgentEval.evaluate(
                List.of(tc),
                List.of(throwingMetric())
        );

        assertThat(result.caseResults()).hasSize(1);
        var caseResult = result.caseResults().getFirst();
        assertThat(caseResult.passed()).isFalse();
        assertThat(caseResult.scores().get("ThrowingMetric").passed()).isFalse();
        assertThat(caseResult.scores().get("ThrowingMetric").reason()).contains("error");
    }

    @Test
    void shouldSetMetricNameOnScores() {
        var tc = AgentTestCase.builder().input("q").actualOutput("a").build();

        var result = AgentEval.evaluate(List.of(tc), List.of(passingMetric()));

        var score = result.caseResults().getFirst().scores().get("PassingMetric");
        assertThat(score.metricName()).isEqualTo("PassingMetric");
    }

    @Test
    void shouldRejectNullTestCases() {
        assertThatThrownBy(() -> AgentEval.evaluate(null, List.of(passingMetric())))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullMetrics() {
        assertThatThrownBy(() -> AgentEval.evaluate(List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldHandleEmptyTestCases() {
        var result = AgentEval.evaluate(List.of(), List.of(passingMetric()));

        assertThat(result.caseResults()).isEmpty();
        assertThat(result.passRate()).isEqualTo(0.0);
    }

    @Test
    void configureShouldReturnConfigBuilder() {
        var config = AgentEval.configure()
                .maxConcurrentJudgeCalls(4)
                .cacheJudgeResults(true)
                .build();

        assertThat(config.maxConcurrentJudgeCalls()).isEqualTo(4);
        assertThat(config.cacheJudgeResults()).isTrue();
    }
}
