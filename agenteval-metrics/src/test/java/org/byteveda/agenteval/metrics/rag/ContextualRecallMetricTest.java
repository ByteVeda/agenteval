package org.byteveda.agenteval.metrics.rag;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContextualRecallMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreHighRecall() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "All expected facts found in context", null));

        var metric = new ContextualRecallMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("any input")
                .expectedOutput("Java was developed by Sun Microsystems and uses JVM.")
                .retrievalContext(List.of("Java was developed by Sun.", "Java uses JVM."))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldRejectMissingRetrievalContext() {
        var metric = new ContextualRecallMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .expectedOutput("expected")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retrievalContext");
    }

    @Test
    void shouldRejectMissingExpectedOutput() {
        var metric = new ContextualRecallMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .retrievalContext(List.of("doc1"))
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedOutput");
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ContextualRecallMetric(judge);
        assertThat(metric.name()).isEqualTo("ContextualRecall");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new ContextualRecallMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("q").expectedOutput("e")
                .retrievalContext(List.of("doc")).build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
