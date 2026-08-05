package org.byteveda.agenteval.metrics.response;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrectnessMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreCorrectOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "Correct answer", null));

        var metric = new CorrectnessMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("What is 2+2?")
                .actualOutput("4")
                .expectedOutput("4")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.9, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreIncorrectOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.1, "Incorrect", null));

        var metric = new CorrectnessMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("What is 2+2?")
                .actualOutput("5")
                .expectedOutput("4")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldRequireExpectedOutput() {
        var metric = new CorrectnessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedOutput");
    }

    @Test
    void shouldAcceptCustomCriteria() {
        when(judge.judge(contains("tone"))).thenReturn(
                new JudgeResponse(0.7, "Acceptable tone", null));

        var metric = new CorrectnessMetric(judge, 0.5,
                "Evaluate the tone of the response", List.of());
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .expectedOutput("expected")
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldAcceptCustomSteps() {
        when(judge.judge(contains("Check grammar"))).thenReturn(
                new JudgeResponse(0.8, "Good", null));

        var metric = new CorrectnessMetric(judge, 0.5, "Evaluate quality",
                List.of("Check grammar", "Check accuracy", "Check completeness"));
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .expectedOutput("expected")
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.5, "OK", null));

        var metric = new CorrectnessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("q").actualOutput("a").expectedOutput("e").build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.5);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new CorrectnessMetric(judge);
        assertThat(metric.name()).isEqualTo("Correctness");
    }
}
