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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrajectoryOptimalityMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreOptimalTrajectory() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "Optimal path taken", null));

        var metric = new TrajectoryOptimalityMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("Find the weather in Paris")
                .actualOutput("The weather in Paris is 22°C and sunny.")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.THOUGHT,"I need to check the weather API"),
                        ReasoningStep.action("Calling weather API",
                                ToolCall.of("getWeather", Map.of("city", "Paris")))))
                .toolCalls(List.of(
                        ToolCall.of("getWeather", Map.of("city", "Paris"))))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreSuboptimalTrajectory() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.3, "Redundant tool calls detected", null));

        var metric = new TrajectoryOptimalityMetric(judge, 0.7);
        var testCase = AgentTestCase.builder()
                .input("Find the weather in Paris")
                .actualOutput("The weather is 22°C.")
                .reasoningTrace(List.of(
                        ReasoningStep.of(ReasoningStepType.THOUGHT,"Let me search for weather"),
                        ReasoningStep.action("Search",
                                ToolCall.of("search", Map.of("q", "weather Paris"))),
                        ReasoningStep.of(ReasoningStepType.THOUGHT,"Let me search again"),
                        ReasoningStep.action("Search again",
                                ToolCall.of("search", Map.of("q", "Paris weather"))),
                        ReasoningStep.action("API call",
                                ToolCall.of("getWeather", Map.of("city", "Paris")))))
                .toolCalls(List.of(
                        ToolCall.of("search", Map.of("q", "weather Paris")),
                        ToolCall.of("search", Map.of("q", "Paris weather")),
                        ToolCall.of("getWeather", Map.of("city", "Paris"))))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldIncludeMaxStepsInPrompt() {
        when(judge.judge(contains("5"))).thenReturn(
                new JudgeResponse(0.8, "Within step limit", null));

        var metric = new TrajectoryOptimalityMetric(judge, 0.7, 5);
        var testCase = AgentTestCase.builder()
                .input("Do something")
                .actualOutput("Done")
                .toolCalls(List.of(ToolCall.of("doIt")))
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldHandleToolCallsOnly() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "Acceptable path", null));

        var metric = new TrajectoryOptimalityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("Fetch data")
                .actualOutput("Data fetched")
                .toolCalls(List.of(
                        ToolCall.of("fetchData", Map.of("id", "123"))))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.value()).isCloseTo(0.7, within(0.001));
    }

    @Test
    void shouldRejectMissingInputAndTrace() {
        var metric = new TrajectoryOptimalityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("Do something")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reasoningTrace or toolCalls");
    }

    @Test
    void shouldRejectEmptyInput() {
        var metric = new TrajectoryOptimalityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("")
                .toolCalls(List.of(ToolCall.of("something")))
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("input");
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new TrajectoryOptimalityMetric(judge);
        assertThat(metric.name()).isEqualTo("TrajectoryOptimality");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.7, "OK", null));

        var metric = new TrajectoryOptimalityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("task")
                .actualOutput("done")
                .toolCalls(List.of(ToolCall.of("doIt")))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.7);
    }
}
