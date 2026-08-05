package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.model.TokenUsage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordingStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTrip() {
        var store = new RecordingStore(tempDir);
        var recording = makeRecording("test-rec");
        store.save(recording);

        Optional<Recording> loaded = store.load("test-rec");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().name()).isEqualTo("test-rec");
        assertThat(loaded.get().interactions()).hasSize(2);
    }

    @Test
    void loadMissingRecordingReturnsEmpty() {
        var store = new RecordingStore(tempDir);
        assertThat(store.load("nonexistent")).isEmpty();
    }

    @Test
    void createsDirectoryAutomatically() {
        Path nested = tempDir.resolve("sub/dir");
        var store = new RecordingStore(nested);
        store.save(makeRecording("auto-dir"));

        assertThat(store.exists("auto-dir")).isTrue();
    }

    @Test
    void existsReturnsTrueForSavedRecording() {
        var store = new RecordingStore(tempDir);
        assertThat(store.exists("missing")).isFalse();

        store.save(makeRecording("exists-test"));
        assertThat(store.exists("exists-test")).isTrue();
    }

    @Test
    void deleteRemovesRecording() {
        var store = new RecordingStore(tempDir);
        store.save(makeRecording("to-delete"));
        assertThat(store.exists("to-delete")).isTrue();

        assertThat(store.delete("to-delete")).isTrue();
        assertThat(store.exists("to-delete")).isFalse();
    }

    @Test
    void deleteNonexistentReturnsFalse() {
        var store = new RecordingStore(tempDir);
        assertThat(store.delete("nope")).isFalse();
    }

    @Test
    void rejectsInvalidNames() {
        var store = new RecordingStore(tempDir);
        assertThatThrownBy(() -> store.save(makeRecording("../evil")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.load(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.load("has spaces"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.exists(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preservesInteractionDetails() {
        var store = new RecordingStore(tempDir);
        var interactions = List.of(
                new RecordedInteraction(
                        InteractionType.AGENT, "hello", "world",
                        TokenUsage.of(5, 10), 1000L),
                new RecordedInteraction(
                        InteractionType.JUDGE, "evaluate", "0.9|good",
                        TokenUsage.of(20, 30), 2000L)
        );
        var recording = new Recording("detail-test", interactions, 3000L);
        store.save(recording);

        Recording loaded = store.load("detail-test").orElseThrow();
        assertThat(loaded.interactions()).hasSize(2);

        RecordedInteraction agent = loaded.interactions().get(0);
        assertThat(agent.type()).isEqualTo(InteractionType.AGENT);
        assertThat(agent.input()).isEqualTo("hello");
        assertThat(agent.output()).isEqualTo("world");
        assertThat(agent.tokenUsage().inputTokens()).isEqualTo(5);
        assertThat(agent.timestampMs()).isEqualTo(1000L);

        RecordedInteraction judge = loaded.interactions().get(1);
        assertThat(judge.type()).isEqualTo(InteractionType.JUDGE);
        assertThat(judge.input()).isEqualTo("evaluate");
    }

    @Test
    void overwritesExistingRecording() {
        var store = new RecordingStore(tempDir);
        store.save(makeRecording("rewrite"));

        var updated = new Recording("rewrite", List.of(
                new RecordedInteraction(
                        InteractionType.AGENT, "new-input", "new-output",
                        null, System.currentTimeMillis())
        ), System.currentTimeMillis());
        store.save(updated);

        Recording loaded = store.load("rewrite").orElseThrow();
        assertThat(loaded.interactions()).hasSize(1);
        assertThat(loaded.interactions().getFirst().input()).isEqualTo("new-input");
    }

    @Test
    void filterMethodsWork() {
        var store = new RecordingStore(tempDir);
        store.save(makeRecording("filter-test"));

        Recording loaded = store.load("filter-test").orElseThrow();
        assertThat(loaded.agentInteractions()).hasSize(1);
        assertThat(loaded.judgeInteractions()).hasSize(1);
        assertThat(loaded.agentInteractions().getFirst().type())
                .isEqualTo(InteractionType.AGENT);
        assertThat(loaded.judgeInteractions().getFirst().type())
                .isEqualTo(InteractionType.JUDGE);
    }

    private static Recording makeRecording(String name) {
        var interactions = List.of(
                new RecordedInteraction(
                        InteractionType.AGENT, "input", "output",
                        TokenUsage.of(10, 20), System.currentTimeMillis()),
                new RecordedInteraction(
                        InteractionType.JUDGE, "prompt", "0.8|good",
                        TokenUsage.of(15, 25), System.currentTimeMillis())
        );
        return new Recording(name, interactions, System.currentTimeMillis());
    }
}
