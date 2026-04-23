package org.byteveda.agenteval.datasets.version;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.DatasetException;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasetVersionerTest {

    @Test
    void shouldTagAndLoadDataset(@TempDir Path tempDir) {
        var dataset = testDataset("my-golden-set");
        var versioner = new DatasetVersioner(tempDir);

        VersionedDataset versioned = versioner.tag(dataset, "v1.0");

        assertThat(versioned.dataset()).isNotNull();
        assertThat(versioned.version().versionLabel()).isEqualTo("v1.0");
        assertThat(versioned.version().createdAt()).isNotNull();
        assertThat(versioned.size()).isEqualTo(2);

        // Load it back
        VersionedDataset loaded = versioner.load("my-golden-set", "v1.0");
        assertThat(loaded.getName()).isEqualTo("my-golden-set");
        assertThat(loaded.getTestCases()).hasSize(2);
        assertThat(loaded.version().versionLabel()).isEqualTo("v1.0");
    }

    @Test
    void shouldListVersionsSortedByNewest(@TempDir Path tempDir) throws Exception {
        var dataset = testDataset("ds");
        var versioner = new DatasetVersioner(tempDir);

        versioner.tag(dataset, "v1.0");
        versioner.tag(dataset, "v2.0");
        // Force deterministic mtime ordering so the test does not depend on
        // clock tick resolution or CI machine speed.
        setVersionMtime(tempDir, "ds", "v1.0", Instant.parse("2020-01-01T00:00:00Z"));
        setVersionMtime(tempDir, "ds", "v2.0", Instant.parse("2020-01-02T00:00:00Z"));

        List<String> versions = versioner.listVersions("ds");
        assertThat(versions).containsExactly("v2.0", "v1.0");
    }

    @Test
    void shouldLoadLatestVersion(@TempDir Path tempDir) throws Exception {
        var dataset = testDataset("ds");
        var versioner = new DatasetVersioner(tempDir);

        versioner.tag(dataset, "v1.0");
        versioner.tag(dataset, "v2.0");
        setVersionMtime(tempDir, "ds", "v1.0", Instant.parse("2020-01-01T00:00:00Z"));
        setVersionMtime(tempDir, "ds", "v2.0", Instant.parse("2020-01-02T00:00:00Z"));

        VersionedDataset latest = versioner.latest("ds");
        assertThat(latest.version().versionLabel()).isEqualTo("v2.0");
    }

    private static void setVersionMtime(Path tempDir, String name, String label, Instant when)
            throws IOException {
        Path versionFile = tempDir.resolve(name).resolve(label).resolve("version.json");
        Files.setLastModifiedTime(versionFile, FileTime.from(when));
    }

    @Test
    void shouldReturnEmptyListForUnknownDataset(@TempDir Path tempDir) {
        var versioner = new DatasetVersioner(tempDir);

        assertThat(versioner.listVersions("nonexistent")).isEmpty();
    }

    @Test
    void shouldThrowWhenLoadingNonexistentVersion(@TempDir Path tempDir) {
        var versioner = new DatasetVersioner(tempDir);

        assertThatThrownBy(() -> versioner.load("ds", "v1.0"))
                .isInstanceOf(DatasetException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldThrowWhenLatestCalledWithNoVersions(@TempDir Path tempDir) {
        var versioner = new DatasetVersioner(tempDir);

        assertThatThrownBy(() -> versioner.latest("ds"))
                .isInstanceOf(DatasetException.class)
                .hasMessageContaining("No versions found");
    }

    @Test
    void shouldThrowWhenDatasetHasNoName(@TempDir Path tempDir) {
        var dataset = EvalDataset.builder()
                .testCases(List.of(AgentTestCase.builder().input("q").build()))
                .build();
        var versioner = new DatasetVersioner(tempDir);

        assertThatThrownBy(() -> versioner.tag(dataset, "v1.0"))
                .isInstanceOf(DatasetException.class)
                .hasMessageContaining("name");
    }

    @Test
    void tagVersionConvenienceMethodShouldWork(@TempDir Path tempDir) {
        var dataset = testDataset("convenience-ds");

        VersionedDataset versioned = dataset.tagVersion("v1.0", tempDir);

        assertThat(versioned.version().versionLabel()).isEqualTo("v1.0");
        assertThat(versioned.getName()).isEqualTo("convenience-ds");
    }

    private static EvalDataset testDataset(String name) {
        return EvalDataset.builder()
                .name(name)
                .testCases(List.of(
                        AgentTestCase.builder().input("What is AI?").build(),
                        AgentTestCase.builder().input("Explain ML").build()))
                .build();
    }
}
