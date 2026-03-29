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

class HallucinationMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreGroundedOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "No hallucinations", null));

        var metric = new HallucinationMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Tell me about Paris")
                .actualOutput("Paris is the capital of France.")
                .context(List.of("Paris is the capital of France."))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreHallucinatedOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.1, "Contains fabricated facts", null));

        var metric = new HallucinationMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Tell me about Paris")
                .actualOutput("Paris has 50 million residents and was founded in 3000 BC.")
                .context(List.of("Paris has about 2 million residents."))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldWorkWithoutContext() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "Some uncertain claims", null));

        var metric = new HallucinationMetric(judge, 0.5, false);
        var testCase = AgentTestCase.builder()
                .input("Tell me about Java")
                .actualOutput("Java was created by James Gosling.")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldRequireContextWhenContextRequired() {
        var metric = new HallucinationMetric(judge, 0.5, true);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("context");
    }

    @Test
    void shouldUseRetrievalContextOverContext() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.8, "Mostly grounded", null));

        var metric = new HallucinationMetric(judge, 0.5, true);
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .retrievalContext(List.of("retrieved doc"))
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new HallucinationMetric(judge);
        assertThat(metric.name()).isEqualTo("Hallucination");
    }
}
