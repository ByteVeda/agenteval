package com.agenteval.metrics.rag;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContextualRelevancyMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreHighRelevancy() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "All documents relevant to query", null));

        var metric = new ContextualRelevancyMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("What is Java?")
                .actualOutput("Java is a programming language.")
                .retrievalContext(List.of("Java is a language.", "Java uses JVM."))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.9, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldRejectMissingInput() {
        var metric = new ContextualRelevancyMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("")
                .retrievalContext(List.of("doc1"))
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("input");
    }

    @Test
    void shouldRejectMissingRetrievalContext() {
        var metric = new ContextualRelevancyMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retrievalContext");
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ContextualRelevancyMetric(judge);
        assertThat(metric.name()).isEqualTo("ContextualRelevancy");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new ContextualRelevancyMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("q").actualOutput("a")
                .retrievalContext(List.of("doc")).build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
