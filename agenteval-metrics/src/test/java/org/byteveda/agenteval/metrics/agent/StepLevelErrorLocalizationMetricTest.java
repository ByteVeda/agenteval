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
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StepLevelErrorLocalizationMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldReturnPerfectScoreWhenAllStepsPass() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "Step is correct", null));

        var tc = AgentTestCase.builder()
                .input("Solve 2+2")
                .expectedOutput("4")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.THOUGHT, "Need to add 2+2"),
                        ReasoningStep.of(ReasoningStepType.ACTION, "Calculate: 2+2=4"),
                        ReasoningStep.of(ReasoningStepType.OBSERVATION, "Result is 4")))
                .build();

        var metric = new StepLevelErrorLocalizationMetric(judge, 0.7);
        EvalScore score = metric.evaluate(tc);

        assertThat(score.value()).isEqualTo(1.0);
        assertThat(score.passed()).isTrue();
        assertThat(score.reason()).contains("All 3 steps are correct");
    }

    @Test
    void shouldDetectFirstFailingStep() {
        when(judge.judge(anyString()))
                .thenReturn(new JudgeResponse(0.9, "Correct", null))
                .thenReturn(new JudgeResponse(0.2, "Incorrect calculation", null));

        var tc = AgentTestCase.builder()
                .input("Solve 2+2")
                .expectedOutput("4")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.THOUGHT, "Need to add"),
                        ReasoningStep.of(ReasoningStepType.ACTION, "Calculate: 2+2=5"),
                        ReasoningStep.of(ReasoningStepType.OBSERVATION, "Result is 5")))
                .build();

        var metric = new StepLevelErrorLocalizationMetric(judge, 0.7);
        EvalScore score = metric.evaluate(tc);

        // First failing step at index 1 out of 3: score = 1/3 ≈ 0.333
        assertThat(score.value()).isCloseTo(0.333, within(0.01));
        assertThat(score.passed()).isFalse();
        assertThat(score.reason()).contains("step 2/3");
    }

    @Test
    void shouldReturnPerfectScoreForEmptyTrace() {
        var tc = AgentTestCase.builder()
                .input("Hello")
                .actualOutput("Hi")
                .build();

        var metric = new StepLevelErrorLocalizationMetric(judge);
        EvalScore score = metric.evaluate(tc);

        assertThat(score.value()).isEqualTo(1.0);
        assertThat(score.reason()).contains("No reasoning trace");
    }

    @Test
    void shouldScoreZeroWhenFirstStepFails() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.1, "Wrong approach", null));

        var tc = AgentTestCase.builder()
                .input("Test")
                .expectedOutput("Expected")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.PLAN, "Bad plan")))
                .build();

        var metric = new StepLevelErrorLocalizationMetric(judge, 0.5);
        EvalScore score = metric.evaluate(tc);

        assertThat(score.value()).isEqualTo(0.0);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new StepLevelErrorLocalizationMetric(judge);
        assertThat(metric.name()).isEqualTo("StepLevelErrorLocalization");
    }
}
