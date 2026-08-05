package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChaosSuiteTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldRunSuiteAndProduceResults() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "Agent handled failure well", null));

        ChaosResult result = ChaosSuite.builder()
                .agent(input -> "I'm sorry, the tool is currently "
                        + "unavailable. Please try again later.")
                .judgeModel(judge)
                .categories(ChaosCategory.TOOL_FAILURE)
                .build()
                .run();

        assertThat(result.totalScenarios()).isGreaterThan(0);
        assertThat(result.overallScore()).isGreaterThan(0.0);
        assertThat(result.results()).isNotEmpty();
    }

    @Test
    void shouldDetectPoorResilience() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.1, "Agent hallucinated data", null));

        ChaosResult result = ChaosSuite.builder()
                .agent(input -> "The weather is sunny and 72F!")
                .judgeModel(judge)
                .categories(ChaosCategory.TOOL_FAILURE)
                .build()
                .run();

        assertThat(result.resilientCount())
                .isLessThan(result.totalScenarios());
        assertThat(result.resilienceRate()).isLessThan(1.0);
    }

    @Test
    void shouldHandleAgentExceptions() {
        ChaosResult result = ChaosSuite.builder()
                .agent(input -> {
                    throw new RuntimeException("Agent crashed");
                })
                .judgeModel(judge)
                .categories(ChaosCategory.TOOL_FAILURE)
                .build()
                .run();

        // Exceptions are treated as non-resilient
        assertThat(result.totalScenarios()).isGreaterThan(0);
        for (var r : result.results()) {
            assertThat(r.resilient()).isFalse();
            assertThat(r.score()).isEqualTo(0.0);
        }
    }

    @Test
    void shouldComputeCategoryScores() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.8, "Good resilience", null));

        ChaosResult result = ChaosSuite.builder()
                .agent(input -> "Service unavailable, please retry")
                .judgeModel(judge)
                .categories(ChaosCategory.TOOL_FAILURE,
                        ChaosCategory.CONTEXT_CORRUPTION)
                .build()
                .run();

        assertThat(result.categoryScores()).isNotEmpty();
        assertThat(result.categoryScores())
                .containsKey(ChaosCategory.TOOL_FAILURE);
        assertThat(result.categoryScores())
                .containsKey(ChaosCategory.CONTEXT_CORRUPTION);
    }

    @Test
    void shouldRunAllCategoriesByDefault() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.85, "Well handled", null));

        ChaosResult result = ChaosSuite.builder()
                .agent(input -> "I encountered an issue and cannot "
                        + "complete this request.")
                .judgeModel(judge)
                .build()
                .run();

        assertThat(result.totalScenarios()).isGreaterThan(5);
    }

    @Test
    void shouldAggregateOverallScoreCorrectly() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.75, "Decent handling", null));

        ChaosResult result = ChaosSuite.builder()
                .agent(input -> "Tool error detected")
                .judgeModel(judge)
                .categories(ChaosCategory.TOOL_FAILURE)
                .build()
                .run();

        assertThat(result.overallScore()).isEqualTo(0.75);
    }
}
