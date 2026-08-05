package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResilienceEvaluatorTest {

    @Test
    void rejectsNullJudge() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ResilienceEvaluator(null))
                .withMessageContaining("judge");
    }

    @Test
    void forwardsRenderedPromptToJudgeAndReturnsItsResponse() {
        JudgeModel judge = mock(JudgeModel.class);
        JudgeResponse expected = new JudgeResponse(0.8, "resilient", null);
        when(judge.judge(anyString())).thenReturn(expected);

        var scenario = new ChaosScenario("tool-fail", ChaosCategory.TOOL_FAILURE,
                "tool returns error", "find weather",
                new ToolFailureInjector("boom"));
        var evaluator = new ResilienceEvaluator(judge);

        JudgeResponse actual = evaluator.evaluate(scenario, "find weather",
                "Sorry, tool is unavailable");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void renderedPromptContainsScenarioAndInteractionFields() {
        JudgeModel judge = mock(JudgeModel.class);
        when(judge.judge(anyString())).thenReturn(new JudgeResponse(1.0, "ok", null));
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        var scenario = new ChaosScenario("ctx-corrupt", ChaosCategory.CONTEXT_CORRUPTION,
                "context shuffled", "hello", new ToolFailureInjector("x"));
        new ResilienceEvaluator(judge).evaluate(scenario, "hello", "hi");

        org.mockito.Mockito.verify(judge).judge(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("CONTEXT_CORRUPTION");
        assertThat(prompt).contains("context shuffled");
        assertThat(prompt).contains("hello");
        assertThat(prompt).contains("hi");
    }

    @Test
    void substitutesNoResponsePlaceholderWhenAgentReturnedNull() {
        JudgeModel judge = mock(JudgeModel.class);
        when(judge.judge(anyString())).thenReturn(new JudgeResponse(0.5, "ok", null));
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        var scenario = new ChaosScenario("lat", ChaosCategory.LATENCY,
                "slow tool", "q", new ToolFailureInjector("x"));
        new ResilienceEvaluator(judge).evaluate(scenario, "q", null);

        org.mockito.Mockito.verify(judge).judge(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("(no response)");
    }
}
