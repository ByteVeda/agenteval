package com.agenteval.judge.multi;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.model.TokenUsage;
import com.agenteval.judge.JudgeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class MultiModelJudgeTest {

    @Test
    void averageStrategyShouldComputeMean() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.8))
                .add(stubJudge("b", 0.6))
                .strategy(ConsensusStrategy.AVERAGE)
                .build();

        JudgeResponse response = judge.judge("test prompt");

        assertThat(response.score()).isCloseTo(0.7, within(0.001));
        assertThat(response.reason()).contains("Average");
    }

    @Test
    void weightedAverageStrategyShouldRespectWeights() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.8), 3.0)
                .add(stubJudge("b", 0.2), 1.0)
                .strategy(ConsensusStrategy.WEIGHTED_AVERAGE)
                .build();

        JudgeResponse response = judge.judge("test prompt");

        // (0.8*3 + 0.2*1) / (3+1) = 2.6/4 = 0.65
        assertThat(response.score()).isCloseTo(0.65, within(0.001));
        assertThat(response.reason()).contains("Weighted average");
    }

    @Test
    void majorityStrategyShouldPassWhenMajorityScoresHigh() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.9))
                .add(stubJudge("b", 0.7))
                .add(stubJudge("c", 0.3))
                .strategy(ConsensusStrategy.MAJORITY)
                .build();

        JudgeResponse response = judge.judge("test prompt");

        // 2/3 judges scored >= 0.5, majority passes
        assertThat(response.score()).isGreaterThanOrEqualTo(0.5);
        assertThat(response.reason()).contains("Majority vote");
    }

    @Test
    void majorityStrategyShouldCapScoreWhenMajorityFails() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.1))
                .add(stubJudge("b", 0.2))
                .add(stubJudge("c", 0.9))
                .strategy(ConsensusStrategy.MAJORITY)
                .build();

        JudgeResponse response = judge.judge("test prompt");

        // Only 1/3 judges scored >= 0.5, majority fails
        assertThat(response.score()).isLessThan(0.5);
    }

    @Test
    void unanimousStrategyShouldPassWhenAllAgree() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.9))
                .add(stubJudge("b", 0.7))
                .strategy(ConsensusStrategy.UNANIMOUS)
                .build();

        JudgeResponse response = judge.judge("test prompt");

        assertThat(response.score()).isCloseTo(0.8, within(0.001));
        assertThat(response.reason()).contains("Unanimous");
    }

    @Test
    void unanimousStrategyShouldReturnZeroWhenNotAllAgree() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.9))
                .add(stubJudge("b", 0.3))
                .strategy(ConsensusStrategy.UNANIMOUS)
                .build();

        JudgeResponse response = judge.judge("test prompt");

        assertThat(response.score()).isEqualTo(0.0);
        assertThat(response.reason()).contains("Not unanimous");
    }

    @Test
    void shouldSumTokenUsageAcrossJudges() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.8, TokenUsage.of(100, 50)))
                .add(stubJudge("b", 0.6, TokenUsage.of(200, 80)))
                .strategy(ConsensusStrategy.AVERAGE)
                .build();

        JudgeResponse response = judge.judge("test prompt");

        assertThat(response.tokenUsage().inputTokens()).isEqualTo(300);
        assertThat(response.tokenUsage().outputTokens()).isEqualTo(130);
        assertThat(response.tokenUsage().totalTokens()).isEqualTo(430);
    }

    @Test
    void shouldHandlePartialJudgeFailure() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.8))
                .add(failingJudge("b"))
                .strategy(ConsensusStrategy.AVERAGE)
                .build();

        JudgeResponse response = judge.judge("test prompt");

        // Only the successful judge contributes
        assertThat(response.score()).isCloseTo(0.8, within(0.001));
    }

    @Test
    void shouldThrowWhenAllJudgesFail() {
        var judge = MultiModelJudge.builder()
                .add(failingJudge("a"))
                .add(failingJudge("b"))
                .strategy(ConsensusStrategy.AVERAGE)
                .build();

        assertThatThrownBy(() -> judge.judge("test prompt"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("All judges failed");
    }

    @Test
    void failOnAnyErrorShouldThrowOnSingleFailure() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.8))
                .add(failingJudge("b"))
                .strategy(ConsensusStrategy.AVERAGE)
                .failOnAnyError(true)
                .build();

        assertThatThrownBy(() -> judge.judge("test prompt"))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("failOnAnyError");
    }

    @Test
    void shouldStoreLastMultiJudgeResponseInThreadLocal() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.8))
                .add(stubJudge("b", 0.6))
                .strategy(ConsensusStrategy.AVERAGE)
                .build();

        judge.judge("test prompt");
        MultiJudgeResponse multiResponse = judge.lastMultiJudgeResponse();

        assertThat(multiResponse).isNotNull();
        assertThat(multiResponse.consensusResponse().score()).isCloseTo(0.7, within(0.001));
        assertThat(multiResponse.individualResults()).hasSize(2);
        assertThat(multiResponse.successfulResults()).hasSize(2);
        assertThat(multiResponse.failedResults()).isEmpty();
    }

    @Test
    void modelIdShouldListAllModels() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("gpt-4o", 0.8))
                .add(stubJudge("claude", 0.6))
                .strategy(ConsensusStrategy.AVERAGE)
                .build();

        assertThat(judge.modelId()).isEqualTo("multi[gpt-4o,claude]");
    }

    @Test
    void builderShouldRejectEmptyJudges() {
        assertThatThrownBy(() -> MultiModelJudge.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one judge");
    }

    @Test
    void defaultWeightShouldBeOne() {
        var judge = MultiModelJudge.builder()
                .add(stubJudge("a", 0.8))
                .strategy(ConsensusStrategy.WEIGHTED_AVERAGE)
                .build();

        judge.judge("test");
        MultiJudgeResponse response = judge.lastMultiJudgeResponse();

        assertThat(response.individualResults().getFirst().weight()).isEqualTo(1.0);
    }

    private static JudgeModel stubJudge(String id, double score) {
        return stubJudge(id, score, TokenUsage.of(10, 5));
    }

    private static JudgeModel stubJudge(String id, double score, TokenUsage usage) {
        return new JudgeModel() {
            @Override
            public JudgeResponse judge(String prompt) {
                return new JudgeResponse(score, "Stub reason from " + id, usage);
            }

            @Override
            public String modelId() {
                return id;
            }
        };
    }

    private static JudgeModel failingJudge(String id) {
        return new JudgeModel() {
            @Override
            public JudgeResponse judge(String prompt) {
                throw new RuntimeException("Simulated failure for " + id);
            }

            @Override
            public String modelId() {
                return id;
            }
        };
    }
}
