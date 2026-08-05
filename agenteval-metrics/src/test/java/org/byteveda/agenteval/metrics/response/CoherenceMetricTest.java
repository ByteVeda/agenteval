package org.byteveda.agenteval.metrics.response;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoherenceMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreCoherentOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "Well-structured and logical", null));

        var metric = new CoherenceMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("Explain photosynthesis")
                .actualOutput("Photosynthesis is the process by which plants convert "
                        + "sunlight into energy. First, chlorophyll absorbs light. "
                        + "Then, this energy drives the conversion of CO2 and water into glucose.")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreIncoherentOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.1, "Contradictory and disorganized", null));

        var metric = new CoherenceMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("Explain gravity")
                .actualOutput("Gravity pulls things down. Also gravity pushes things up. "
                        + "Cats are nice. The moon is made of cheese.")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldRejectMissingInput() {
        var metric = new CoherenceMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("")
                .actualOutput("answer")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMissingOutput() {
        var metric = new CoherenceMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new CoherenceMetric(judge);
        assertThat(metric.name()).isEqualTo("Coherence");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new CoherenceMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("q").actualOutput("a").build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
