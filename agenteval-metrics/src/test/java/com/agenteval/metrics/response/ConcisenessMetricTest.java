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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConcisenessMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreConciseOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "Very concise response", null));

        var metric = new ConcisenessMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("What is 2+2?")
                .actualOutput("4")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreVerboseOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.2, "Extremely verbose", null));

        var metric = new ConcisenessMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("What is 2+2?")
                .actualOutput("Well, let me think about that. The answer is 4. "
                        + "As you may know, addition is a mathematical operation...")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldRejectMissingInput() {
        var metric = new ConcisenessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("")
                .actualOutput("answer")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingOutput() {
        var metric = new ConcisenessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ConcisenessMetric(judge);
        assertThat(metric.name()).isEqualTo("Conciseness");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.5, "OK", null));

        var metric = new ConcisenessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("q").actualOutput("a").build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.5);
    }
}
