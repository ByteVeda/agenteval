package org.byteveda.agenteval.metrics.conversation;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ConversationTestCase;
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

class ConversationCoherenceMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreCoherentConversation() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "Coherent conversation", null));

        var metric = new ConversationCoherenceMetric(judge, 0.7);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("What is Java?")
                                .actualOutput("Java is a programming language.")
                                .build(),
                        AgentTestCase.builder()
                                .input("What about Python?")
                                .actualOutput("Python is also a programming language.")
                                .build()))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.9, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldIncludeSystemPromptInEvaluation() {
        when(judge.judge(contains("helpful assistant"))).thenReturn(
                new JudgeResponse(0.8, "Consistent with system prompt", null));

        var metric = new ConversationCoherenceMetric(judge);
        var testCase = ConversationTestCase.builder()
                .systemPrompt("You are a helpful assistant")
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("Hello")
                                .actualOutput("Hi there!")
                                .build()))
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldHandleMissingSystemPrompt() {
        when(judge.judge(contains("(none)"))).thenReturn(
                new JudgeResponse(0.8, "OK", null));

        var metric = new ConversationCoherenceMetric(judge);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("Hello")
                                .actualOutput("Hi")
                                .build()))
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldRejectNullTestCase() {
        var metric = new ConversationCoherenceMetric(judge);
        assertThatThrownBy(() -> metric.evaluate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ConversationCoherenceMetric(judge);
        assertThat(metric.name()).isEqualTo("ConversationCoherence");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new ConversationCoherenceMetric(judge);
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("q").actualOutput("a").build()))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
