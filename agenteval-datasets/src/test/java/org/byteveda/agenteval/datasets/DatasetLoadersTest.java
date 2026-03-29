package org.byteveda.agenteval.datasets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatasetLoadersTest {

    @Test
    void shouldAutoDetectJson(@TempDir Path tmpDir) throws Exception {
        Path file = tmpDir.resolve("data.json");
        Files.writeString(file, "[{\"input\":\"Hello\"}]");

        EvalDataset dataset = DatasetLoaders.forPath(file);
        assertThat(dataset.size()).isEqualTo(1);
    }

    @Test
    void shouldAutoDetectJsonl(@TempDir Path tmpDir) throws Exception {
        Path file = tmpDir.resolve("data.jsonl");
        Files.writeString(file, "{\"input\":\"Hello\"}\n");

        EvalDataset dataset = DatasetLoaders.forPath(file);
        assertThat(dataset.size()).isEqualTo(1);
    }

    @Test
    void shouldAutoDetectCsv(@TempDir Path tmpDir) throws Exception {
        Path file = tmpDir.resolve("data.csv");
        Files.writeString(file, "input,actualOutput\nHello,World\n");

        EvalDataset dataset = DatasetLoaders.forPath(file);
        assertThat(dataset.size()).isEqualTo(1);
    }

    @Test
    void shouldThrowOnUnsupportedFormat(@TempDir Path tmpDir) throws Exception {
        Path file = tmpDir.resolve("data.xml");
        Files.writeString(file, "<data/>");

        assertThatThrownBy(() -> DatasetLoaders.forPath(file))
                .isInstanceOf(DatasetException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void shouldDetectFormats() {
        assertThat(DatasetFormat.detect(Path.of("data.json"))).isEqualTo(DatasetFormat.JSON);
        assertThat(DatasetFormat.detect(Path.of("data.jsonl"))).isEqualTo(DatasetFormat.JSONL);
        assertThat(DatasetFormat.detect(Path.of("data.csv"))).isEqualTo(DatasetFormat.CSV);
    }
}
