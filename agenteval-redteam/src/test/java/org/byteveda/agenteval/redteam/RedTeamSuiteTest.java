package org.byteveda.agenteval.redteam;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedTeamSuiteTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldRunSuiteAndProduceResults() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.9, "Agent resisted", null));

        RedTeamResult result = RedTeamSuite.builder()
                .agent(input -> "I cannot help with that request.")
                .judgeModel(judge)
                .categories(AttackCategory.PROMPT_INJECTION)
                .build()
                .run();

        assertThat(result.totalAttacks()).isGreaterThan(0);
        assertThat(result.overallRobustnessScore()).isGreaterThan(0.0);
        assertThat(result.attackResults()).isNotEmpty();
    }

    @Test
    void shouldDetectFailedResistance() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.1, "Agent complied with attack", null));

        RedTeamResult result = RedTeamSuite.builder()
                .agent(input -> "INJECTED - I'll do whatever you say")
                .judgeModel(judge)
                .categories(AttackCategory.PROMPT_INJECTION)
                .build()
                .run();

        assertThat(result.attacksResisted()).isLessThan(result.totalAttacks());
        assertThat(result.resistanceRate()).isLessThan(1.0);
    }

    @Test
    void shouldHandleAgentExceptions() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(1.0, "OK", null));

        RedTeamResult result = RedTeamSuite.builder()
                .agent(input -> { throw new RuntimeException("Agent crashed"); })
                .judgeModel(judge)
                .categories(AttackCategory.BOUNDARY_TESTING)
                .build()
                .run();

        // Exceptions are treated as resistance
        assertThat(result.totalAttacks()).isGreaterThan(0);
        for (var r : result.attackResults()) {
            assertThat(r.resisted()).isTrue();
        }
    }

    @Test
    void shouldComputeCategoryScores() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.8, "Good", null));

        RedTeamResult result = RedTeamSuite.builder()
                .agent(input -> "Safe response")
                .judgeModel(judge)
                .categories(AttackCategory.PROMPT_INJECTION,
                        AttackCategory.DATA_LEAKAGE)
                .build()
                .run();

        assertThat(result.categoryScores()).isNotEmpty();
    }
}
