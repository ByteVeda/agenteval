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

class ContextualPrecisionMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreHighPrecision() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "All documents relevant", null));

        var metric = new ContextualPrecisionMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("What is Java?")
                .actualOutput("Java is a programming language.")
                .expectedOutput("Java is a programming language developed by Sun Microsystems.")
                .retrievalContext(List.of("Java was developed by Sun.", "Java uses JVM."))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.9, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldRejectMissingRetrievalContext() {
        var metric = new ContextualPrecisionMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .expectedOutput("expected")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retrievalContext");
    }

    @Test
    void shouldRejectMissingExpectedOutput() {
        var metric = new ContextualPrecisionMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .retrievalContext(List.of("doc1"))
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedOutput");
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ContextualPrecisionMetric(judge);
        assertThat(metric.name()).isEqualTo("ContextualPrecision");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new ContextualPrecisionMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("q").actualOutput("a").expectedOutput("e")
                .retrievalContext(List.of("doc")).build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }

    @Test
    void shouldFormatContextWithNumberedSeparators() {
        String formatted = ContextualPrecisionMetric.formatContext(
                List.of("first doc", "second doc", "third doc"));
        assertThat(formatted).isEqualTo("[1] first doc\n[2] second doc\n[3] third doc");
    }
}
