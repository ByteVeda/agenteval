package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
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

class TaskCompletionMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreCompletedTask() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.95, "Task fully completed", null));

        var metric = new TaskCompletionMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Book a flight to Paris")
                .actualOutput("Flight booked: LAX to CDG on March 15")
                .toolCalls(List.of(
                        ToolCall.of("searchFlights", Map.of("destination", "Paris")),
                        ToolCall.of("bookFlight", Map.of("flightId", "AF123"))))
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.95, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreIncompleteTask() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.3, "Task not completed", null));

        var metric = new TaskCompletionMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Book a flight to Paris")
                .actualOutput("I found some flights but couldn't complete the booking.")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldIncludeSuccessCriteria() {
        when(judge.judge(contains("confirmation number"))).thenReturn(
                new JudgeResponse(0.8, "Met criteria", null));

        var metric = new TaskCompletionMetric(judge, 0.5,
                "Output must include a confirmation number");
        var testCase = AgentTestCase.builder()
                .input("Book a flight")
                .actualOutput("Booked. Confirmation: ABC123")
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldHandleNoToolCalls() {
        when(judge.judge(contains("(none)"))).thenReturn(
                new JudgeResponse(0.5, "Completed without tools", null));

        var metric = new TaskCompletionMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Answer a question")
                .actualOutput("Here is the answer.")
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldRejectMissingOutput() {
        var metric = new TaskCompletionMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("Do something")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new TaskCompletionMetric(judge);
        assertThat(metric.name()).isEqualTo("TaskCompletion");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.5, "OK", null));

        var metric = new TaskCompletionMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("task").actualOutput("done").build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.5);
    }
}
