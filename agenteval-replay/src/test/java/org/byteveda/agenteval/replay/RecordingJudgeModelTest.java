package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordingJudgeModelTest {

    @Test
    void shouldDelegateToUnderlyingJudge() {
        JudgeModel delegate = mock(JudgeModel.class);
        var response = new JudgeResponse(0.9, "good answer", TokenUsage.of(10, 20));
        when(delegate.judge("test prompt")).thenReturn(response);

        var recording = new RecordingJudgeModel(delegate);
        JudgeResponse result = recording.judge("test prompt");

        assertThat(result.score()).isEqualTo(0.9);
        assertThat(result.reason()).isEqualTo("good answer");
        verify(delegate).judge("test prompt");
    }

    @Test
    void shouldRecordInteractions() {
        JudgeModel delegate = mock(JudgeModel.class);
        var response1 = new JudgeResponse(0.8, "decent", TokenUsage.of(5, 10));
        var response2 = new JudgeResponse(0.95, "excellent", TokenUsage.of(8, 15));
        when(delegate.judge("prompt1")).thenReturn(response1);
        when(delegate.judge("prompt2")).thenReturn(response2);

        var recording = new RecordingJudgeModel(delegate);
        recording.judge("prompt1");
        recording.judge("prompt2");

        List<RecordedInteraction> interactions = recording.getInteractions();
        assertThat(interactions).hasSize(2);
        assertThat(interactions.get(0).type()).isEqualTo(InteractionType.JUDGE);
        assertThat(interactions.get(0).input()).isEqualTo("prompt1");
        assertThat(interactions.get(0).output()).isEqualTo("0.8|decent");
        assertThat(interactions.get(1).input()).isEqualTo("prompt2");
        assertThat(interactions.get(1).output()).isEqualTo("0.95|excellent");
    }

    @Test
    void shouldDelegateModelId() {
        JudgeModel delegate = mock(JudgeModel.class);
        when(delegate.modelId()).thenReturn("gpt-4o");

        var recording = new RecordingJudgeModel(delegate);
        assertThat(recording.modelId()).isEqualTo("gpt-4o");
    }

    @Test
    void shouldTrackSize() {
        JudgeModel delegate = mock(JudgeModel.class);
        var response = new JudgeResponse(0.5, "ok", TokenUsage.of(1, 1));
        when(delegate.judge("p")).thenReturn(response);

        var recording = new RecordingJudgeModel(delegate);
        assertThat(recording.size()).isZero();

        recording.judge("p");
        assertThat(recording.size()).isEqualTo(1);
    }

    @Test
    void shouldClearInteractions() {
        JudgeModel delegate = mock(JudgeModel.class);
        var response = new JudgeResponse(0.7, "fine", TokenUsage.of(1, 1));
        when(delegate.judge("p")).thenReturn(response);

        var recording = new RecordingJudgeModel(delegate);
        recording.judge("p");
        assertThat(recording.size()).isEqualTo(1);

        recording.clear();
        assertThat(recording.size()).isZero();
        assertThat(recording.getInteractions()).isEmpty();
    }

    @Test
    void shouldRejectNullDelegate() {
        assertThatThrownBy(() -> new RecordingJudgeModel(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldPreserveTokenUsage() {
        JudgeModel delegate = mock(JudgeModel.class);
        var usage = TokenUsage.of(100, 200);
        var response = new JudgeResponse(0.6, "reason", usage);
        when(delegate.judge("prompt")).thenReturn(response);

        var recording = new RecordingJudgeModel(delegate);
        recording.judge("prompt");

        RecordedInteraction interaction = recording.getInteractions().getFirst();
        assertThat(interaction.tokenUsage()).isEqualTo(usage);
    }
}
