package com.agenteval.datasets;

import com.agenteval.core.model.AgentTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvalDatasetTest {

    @Test
    void shouldBuildDatasetWithAllFields() {
        var tc = AgentTestCase.builder().input("test input").build();
        var dataset = EvalDataset.builder()
                .name("my-dataset")
                .version("2.0")
                .testCases(List.of(tc))
                .metadata(Map.of("key", "value"))
                .build();

        assertThat(dataset.getName()).isEqualTo("my-dataset");
        assertThat(dataset.getVersion()).isEqualTo("2.0");
        assertThat(dataset.getTestCases()).hasSize(1);
        assertThat(dataset.getMetadata()).containsEntry("key", "value");
        assertThat(dataset.size()).isEqualTo(1);
    }

    @Test
    void shouldBuildDatasetWithMinimalFields() {
        var dataset = EvalDataset.builder()
                .testCases(List.of(AgentTestCase.builder().input("q").build()))
                .build();

        assertThat(dataset.getName()).isNull();
        assertThat(dataset.getVersion()).isNull();
        assertThat(dataset.getTestCases()).hasSize(1);
        assertThat(dataset.getMetadata()).isEmpty();
    }

    @Test
    void shouldRejectNullTestCases() {
        assertThatThrownBy(() -> EvalDataset.builder().build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("testCases");
    }

    @Test
    void shouldDefensiveCopyTestCases() {
        var list = new java.util.ArrayList<>(
                List.of(AgentTestCase.builder().input("q").build()));
        var dataset = EvalDataset.builder().testCases(list).build();
        list.add(AgentTestCase.builder().input("extra").build());
        assertThat(dataset.getTestCases()).hasSize(1);
    }

    @Test
    void shouldSaveToFile(@TempDir Path tempDir) {
        var dataset = EvalDataset.builder()
                .name("save-test")
                .testCases(List.of(AgentTestCase.builder().input("q").build()))
                .build();

        Path outFile = tempDir.resolve("out.json");
        dataset.save(outFile);
        assertThat(outFile).exists().isNotEmptyFile();
    }
}
