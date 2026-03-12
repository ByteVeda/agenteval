package com.agenteval.metrics.agent;

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

class RetrievalCompletenessMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreExactMatchPerfect() {
        var metric = new RetrievalCompletenessMetric(judge, 0.8,
                RetrievalCompletenessMetric.MatchMode.EXACT);
        var testCase = AgentTestCase.builder()
                .input("query")
                .context(List.of("doc1", "doc2"))
                .retrievalContext(List.of("doc1", "doc2", "doc3"))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreExactMatchPartial() {
        var metric = new RetrievalCompletenessMetric(judge, 0.8,
                RetrievalCompletenessMetric.MatchMode.EXACT);
        var testCase = AgentTestCase.builder()
                .input("query")
                .context(List.of("doc1", "doc2"))
                .retrievalContext(List.of("doc1", "other"))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.5, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldScoreSemanticMode() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.85, "Good coverage", null));

        var metric = new RetrievalCompletenessMetric(judge, 0.8,
                RetrievalCompletenessMetric.MatchMode.SEMANTIC);
        var testCase = AgentTestCase.builder()
                .input("query")
                .context(List.of("expected doc"))
                .retrievalContext(List.of("retrieved doc"))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.85, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldRejectMissingContext() {
        var metric = new RetrievalCompletenessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("query")
                .retrievalContext(List.of("doc1"))
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("context");
    }

    @Test
    void shouldRejectMissingRetrievalContext() {
        var metric = new RetrievalCompletenessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("query")
                .context(List.of("doc1"))
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retrievalContext");
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new RetrievalCompletenessMetric(judge);
        assertThat(metric.name()).isEqualTo("RetrievalCompleteness");
    }

    @Test
    void shouldDefaultToSemanticMode() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.8, "OK", null));

        var metric = new RetrievalCompletenessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("query")
                .context(List.of("doc"))
                .retrievalContext(List.of("doc"))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.8);
    }
}
