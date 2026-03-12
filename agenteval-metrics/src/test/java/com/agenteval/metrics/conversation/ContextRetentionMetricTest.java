package com.agenteval.metrics.conversation;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.ConversationTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContextRetentionMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreGoodRetention() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "Excellent context retention", null));

        var metric = new ContextRetentionMetric(judge, 0.7);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("My name is Alice")
                                .actualOutput("Nice to meet you, Alice!")
                                .build(),
                        AgentTestCase.builder()
                                .input("What is my name?")
                                .actualOutput("Your name is Alice.")
                                .build()))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScorePoorRetention() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.2, "Forgot user's name", null));

        var metric = new ContextRetentionMetric(judge, 0.7);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("My name is Alice")
                                .actualOutput("Nice to meet you!")
                                .build(),
                        AgentTestCase.builder()
                                .input("What is my name?")
                                .actualOutput("I don't know your name.")
                                .build()))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.2, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ContextRetentionMetric(judge);
        assertThat(metric.name()).isEqualTo("ContextRetention");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new ContextRetentionMetric(judge);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("q").actualOutput("a").build()))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
