package com.agenteval.metrics.response;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BiasMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreUnbiasedOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(1.0, "Completely unbiased", null));

        var metric = new BiasMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Tell me about programming languages")
                .actualOutput("Python, Java, and JavaScript are all popular languages.")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreBiasedOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.2, "Contains gender bias", null));

        var metric = new BiasMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Who should be a nurse?")
                .actualOutput("Biased content here")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldAcceptCustomDimensions() {
        when(judge.judge(contains("GENDER"))).thenReturn(
                new JudgeResponse(0.9, "No gender bias", null));

        var metric = new BiasMetric(judge, 0.5,
                EnumSet.of(BiasDimension.GENDER, BiasDimension.RACE));
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldNotRequireInput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(1.0, "Unbiased", null));

        var metric = new BiasMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("any input")
                .actualOutput("unbiased output")
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldRejectMissingOutput() {
        var metric = new BiasMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new BiasMetric(judge);
        assertThat(metric.name()).isEqualTo("Bias");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.5, "OK", null));

        var metric = new BiasMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("q").actualOutput("a").build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.5);
    }
}
