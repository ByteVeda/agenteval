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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConversationResolutionMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreHighForResolvedConversation() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "Fully resolved", null));

        var metric = new ConversationResolutionMetric(judge, 0.7);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("How do I reset my password?")
                                .actualOutput("Go to Settings > Security > Reset Password")
                                .build(),
                        AgentTestCase.builder()
                                .input("Thanks, that worked!")
                                .actualOutput("Glad I could help!")
                                .build()))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreLowForUnresolvedConversation() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.2, "Issue not addressed", null));

        var metric = new ConversationResolutionMetric(judge, 0.7);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("My account is locked")
                                .actualOutput("I understand you have an issue")
                                .build()))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.2, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldIncludeSuccessCriteriaInPrompt() {
        when(judge.judge(contains("user confirmed"))).thenReturn(
                new JudgeResponse(0.8, "Criteria met", null));

        var metric = new ConversationResolutionMetric(judge, 0.7, "user confirmed resolution");
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("Help me")
                                .actualOutput("Here's the solution")
                                .build()))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(0.8, within(0.001));
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ConversationResolutionMetric(judge);
        assertThat(metric.name()).isEqualTo("ConversationResolution");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new ConversationResolutionMetric(judge);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("q").actualOutput("a").build()))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
