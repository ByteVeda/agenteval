package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplayJudgeModelTest {

    @Test
    void shouldReplayMatchingPrompt() {
        var interaction = new RecordedInteraction(
                InteractionType.JUDGE,
                "evaluate this",
                "0.85|good response",
                TokenUsage.of(10, 20),
                System.currentTimeMillis()
        );
        var recording = new Recording("test", List.of(interaction), System.currentTimeMillis());
        var replay = new ReplayJudgeModel(recording, "test-model");

        JudgeResponse response = replay.judge("evaluate this");
        assertThat(response.score()).isEqualTo(0.85);
        assertThat(response.reason()).isEqualTo("good response");
        assertThat(response.tokenUsage()).isEqualTo(TokenUsage.of(10, 20));
    }

    @Test
    void shouldReturnModelId() {
        var recording = new Recording("test", List.of(), System.currentTimeMillis());
        var replay = new ReplayJudgeModel(recording, "claude-sonnet-4-20250514");

        assertThat(replay.modelId()).isEqualTo("claude-sonnet-4-20250514");
    }

    @Test
    void shouldThrowOnMismatch() {
        var interaction = new RecordedInteraction(
                InteractionType.JUDGE,
                "recorded prompt",
                "0.5|ok",
                null,
                System.currentTimeMillis()
        );
        var recording = new Recording("test", List.of(interaction), System.currentTimeMillis());
        var replay = new ReplayJudgeModel(recording, "model");

        assertThatThrownBy(() -> replay.judge("different prompt"))
                .isInstanceOf(ReplayMismatchException.class)
                .hasMessageContaining("No recorded judge interaction found");
    }

    @Test
    void shouldHandleMultipleInteractions() {
        var interaction1 = new RecordedInteraction(
                InteractionType.JUDGE, "prompt-a", "0.9|great",
                null, System.currentTimeMillis());
        var interaction2 = new RecordedInteraction(
                InteractionType.JUDGE, "prompt-b", "0.3|poor",
                null, System.currentTimeMillis());
        var recording = new Recording("test",
                List.of(interaction1, interaction2), System.currentTimeMillis());
        var replay = new ReplayJudgeModel(recording, "model");

        assertThat(replay.judge("prompt-b").score()).isEqualTo(0.3);
        assertThat(replay.judge("prompt-a").score()).isEqualTo(0.9);
    }

    @Test
    void shouldFilterOutAgentInteractions() {
        var agentInteraction = new RecordedInteraction(
                InteractionType.AGENT, "input", "output",
                null, System.currentTimeMillis());
        var judgeInteraction = new RecordedInteraction(
                InteractionType.JUDGE, "prompt", "0.7|ok",
                null, System.currentTimeMillis());
        var recording = new Recording("test",
                List.of(agentInteraction, judgeInteraction), System.currentTimeMillis());
        var replay = new ReplayJudgeModel(recording, "model");

        JudgeResponse response = replay.judge("prompt");
        assertThat(response.score()).isEqualTo(0.7);
    }

    @Test
    void shouldRejectNullRecording() {
        assertThatThrownBy(() -> new ReplayJudgeModel(null, "model"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullModelId() {
        var recording = new Recording("test", List.of(), System.currentTimeMillis());
        assertThatThrownBy(() -> new ReplayJudgeModel(recording, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldHandleNullTokenUsage() {
        var interaction = new RecordedInteraction(
                InteractionType.JUDGE,
                "prompt",
                "0.5|reason",
                null,
                System.currentTimeMillis()
        );
        var recording = new Recording("test", List.of(interaction), System.currentTimeMillis());
        var replay = new ReplayJudgeModel(recording, "model");

        JudgeResponse response = replay.judge("prompt");
        assertThat(response.score()).isEqualTo(0.5);
        assertThat(response.tokenUsage()).isNull();
    }
}
