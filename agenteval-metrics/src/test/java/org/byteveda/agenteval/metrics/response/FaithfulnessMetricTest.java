package org.byteveda.agenteval.metrics.response;

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

class FaithfulnessMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreFaithfulOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "All claims supported", null));

        var metric = new FaithfulnessMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("What is the capital of France?")
                .actualOutput("Paris is the capital of France.")
                .retrievalContext(List.of("France is a country in Europe. Its capital is Paris."))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreUnfaithfulOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.2, "Claims not supported by context", null));

        var metric = new FaithfulnessMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("What is the capital of France?")
                .actualOutput("London is the capital of France.")
                .retrievalContext(List.of("France is a country. Its capital is Paris."))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldRequireRetrievalContext() {
        var metric = new FaithfulnessMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retrievalContext");
    }

    @Test
    void shouldJoinMultipleContextDocuments() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.8, "Mostly faithful", null));

        var metric = new FaithfulnessMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .retrievalContext(List.of("doc1", "doc2", "doc3"))
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new FaithfulnessMetric(judge);
        assertThat(metric.name()).isEqualTo("Faithfulness");
    }
}
