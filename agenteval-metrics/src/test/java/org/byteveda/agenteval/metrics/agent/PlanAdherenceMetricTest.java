package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.ReasoningStep;
import org.byteveda.agenteval.core.model.ReasoningStepType;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanAdherenceMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreGoodAdherence() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "Followed plan exactly", null));

        var metric = new PlanAdherenceMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("Search for restaurants")
                .actualOutput("Found 3 restaurants nearby")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.PLAN, "Search for restaurants"),
                        ReasoningStep.of(ReasoningStepType.ACTION, "Called search API")))
                .toolCalls(List.of(ToolCall.of("search")))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldRejectEmptyReasoningTrace() {
        var metric = new PlanAdherenceMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("task")
                .actualOutput("result")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reasoningTrace");
    }

    @Test
    void shouldRejectMissingOutput() {
        var metric = new PlanAdherenceMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("task")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.PLAN, "step")))
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new PlanAdherenceMetric(judge);
        assertThat(metric.name()).isEqualTo("PlanAdherence");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new PlanAdherenceMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("task").actualOutput("result")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.PLAN, "step")))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
