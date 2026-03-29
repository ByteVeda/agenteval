package org.byteveda.agenteval.reporting.snapshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class SnapshotStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTrip() {
        var store = new SnapshotStore(tempDir);
        var snapshot = makeSnapshot("test-snap", 0.85, 1.0, 1);
        store.save(snapshot);

        Optional<SnapshotData> loaded = store.load("test-snap");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().snapshotName()).isEqualTo("test-snap");
        assertThat(loaded.get().averageScore()).isCloseTo(0.85, within(0.001));
        assertThat(loaded.get().passRate()).isCloseTo(1.0, within(0.001));
        assertThat(loaded.get().totalCases()).isEqualTo(1);
    }

    @Test
    void loadMissingSnapshotReturnsEmpty() {
        var store = new SnapshotStore(tempDir);
        assertThat(store.load("nonexistent")).isEmpty();
    }

    @Test
    void createsDirectoryAutomatically() {
        Path nested = tempDir.resolve("sub/dir");
        var store = new SnapshotStore(nested);
        store.save(makeSnapshot("auto-dir", 0.9, 1.0, 1));

        assertThat(store.exists("auto-dir")).isTrue();
    }

    @Test
    void existsReturnsTrueForSavedSnapshot() {
        var store = new SnapshotStore(tempDir);
        assertThat(store.exists("missing")).isFalse();

        store.save(makeSnapshot("exists-test", 0.9, 1.0, 1));
        assertThat(store.exists("exists-test")).isTrue();
    }

    @Test
    void deleteRemovesSnapshot() {
        var store = new SnapshotStore(tempDir);
        store.save(makeSnapshot("to-delete", 0.9, 1.0, 1));
        assertThat(store.exists("to-delete")).isTrue();

        assertThat(store.delete("to-delete")).isTrue();
        assertThat(store.exists("to-delete")).isFalse();
    }

    @Test
    void deleteNonexistentReturnsFalse() {
        var store = new SnapshotStore(tempDir);
        assertThat(store.delete("nope")).isFalse();
    }

    @Test
    void rejectsInvalidNames() {
        var store = new SnapshotStore(tempDir);
        assertThatThrownBy(() -> store.save(makeSnapshot("../evil", 0.0, 0.0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.load(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.load("has spaces"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.exists(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multiCaseSnapshotPreservesAllData() {
        var store = new SnapshotStore(tempDir);
        var cases = List.of(
                new SnapshotCaseData("input1", "output1", true,
                        Map.of("Metric1", new SnapshotScoreData(0.9, 0.7, true, "good"))),
                new SnapshotCaseData("input2", "output2", false,
                        Map.of("Metric1", new SnapshotScoreData(0.5, 0.7, false, "poor")))
        );
        var snapshot = new SnapshotData("multi", Instant.now(), 0.7, 0.5, 2, 200L,
                Map.of("Metric1", 0.7), cases);

        store.save(snapshot);
        SnapshotData loaded = store.load("multi").orElseThrow();

        assertThat(loaded.caseResults()).hasSize(2);
        assertThat(loaded.caseResults().get(0).input()).isEqualTo("input1");
        assertThat(loaded.caseResults().get(1).passed()).isFalse();
        assertThat(loaded.caseResults().get(1).scores().get("Metric1").value())
                .isCloseTo(0.5, within(0.001));
    }

    @Test
    void overwritesExistingSnapshot() {
        var store = new SnapshotStore(tempDir);
        store.save(makeSnapshot("rewrite", 0.5, 0.5, 1));
        store.save(makeSnapshot("rewrite", 0.9, 1.0, 1));

        SnapshotData loaded = store.load("rewrite").orElseThrow();
        assertThat(loaded.averageScore()).isCloseTo(0.9, within(0.001));
    }

    private static SnapshotData makeSnapshot(String name, double avg, double pass, int cases) {
        List<SnapshotCaseData> caseList = List.of(
                new SnapshotCaseData("input", "output", pass == 1.0,
                        Map.of("TestMetric", new SnapshotScoreData(avg, 0.7, avg >= 0.7, "ok")))
        );
        return new SnapshotData(name, Instant.now(), avg, pass, cases, 100L,
                Map.of("TestMetric", avg), caseList);
    }
}
