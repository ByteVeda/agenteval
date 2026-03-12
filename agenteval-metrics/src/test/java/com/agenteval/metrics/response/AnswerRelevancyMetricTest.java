package com.agenteval.metrics.response;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnswerRelevancyMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreRelevantAnswer() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "Highly relevant response", null));

        var metric = new AnswerRelevancyMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("What is Java?")
                .actualOutput("Java is a programming language created by Sun Microsystems.")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.9, within(0.001));
        assertThat(score.passed()).isTrue();
        assertThat(score.reason()).contains("relevant");
    }

    @Test
    void shouldScoreIrrelevantAnswer() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.2, "Off-topic response", null));

        var metric = new AnswerRelevancyMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("What is Java?")
                .actualOutput("I like pizza.")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.2, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldIncludeStrictModeInPrompt() {
        when(judge.judge(contains("strict mode"))).thenReturn(
                new JudgeResponse(0.6, "Strict evaluation", null));

        var metric = new AnswerRelevancyMetric(judge, 0.7, true);
        var testCase = AgentTestCase.builder()
                .input("What is Java?")
                .actualOutput("Java is a language. Also, cats are nice.")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(0.6, within(0.001));
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.75, "Good", null));

        var metric = new AnswerRelevancyMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new AnswerRelevancyMetric(judge);
        assertThat(metric.name()).isEqualTo("AnswerRelevancy");
    }

    @Test
    void shouldRejectMissingInput() {
        var metric = new AnswerRelevancyMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("")
                .actualOutput("answer")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingOutput() {
        var metric = new AnswerRelevancyMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
