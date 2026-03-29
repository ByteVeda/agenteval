package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.ReasoningStep;
import org.byteveda.agenteval.core.model.ReasoningStepType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanQualityMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreGoodPlan() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "Clear and well-ordered plan", null));

        var metric = new PlanQualityMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("Book a flight to Paris")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.PLAN, "Search for flights"),
                        ReasoningStep.of(ReasoningStepType.PLAN, "Compare prices"),
                        ReasoningStep.of(ReasoningStepType.PLAN, "Book cheapest option")))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.9, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldRejectEmptyReasoningTrace() {
        var metric = new PlanQualityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("task")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reasoningTrace");
    }

    @Test
    void shouldRejectMissingInput() {
        var metric = new PlanQualityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.PLAN, "step")))
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("input");
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new PlanQualityMetric(judge);
        assertThat(metric.name()).isEqualTo("PlanQuality");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new PlanQualityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("task")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.PLAN, "step")))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
