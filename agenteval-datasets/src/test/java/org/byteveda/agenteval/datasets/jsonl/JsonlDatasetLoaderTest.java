package org.byteveda.agenteval.datasets.jsonl;

import org.byteveda.agenteval.datasets.DatasetException;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonlDatasetLoaderTest {

    @Test
    void shouldLoadFromClasspath() {
        var is = getClass().getClassLoader().getResourceAsStream("test-dataset.jsonl");
        EvalDataset dataset = new JsonlDatasetLoader().load(is);

        assertThat(dataset.size()).isEqualTo(2);
        assertThat(dataset.getTestCases().get(0).getInput()).isEqualTo("What is Java?");
        assertThat(dataset.getTestCases().get(0).getRetrievalContext()).hasSize(2);
    }

    @Test
    void shouldLoadFromPath(@TempDir Path tmpDir) throws Exception {
        Path jsonlFile = tmpDir.resolve("test.jsonl");
        Files.writeString(jsonlFile, "{\"input\":\"Hello\",\"actualOutput\":\"World\"}\n");

        EvalDataset dataset = new JsonlDatasetLoader().load(jsonlFile);

        assertThat(dataset.size()).isEqualTo(1);
        assertThat(dataset.getTestCases().get(0).getInput()).isEqualTo("Hello");
    }

    @Test
    void shouldSkipBlankLines() {
        String jsonl = "{\"input\":\"q1\"}\n\n{\"input\":\"q2\"}\n";
        var is = new ByteArrayInputStream(jsonl.getBytes(StandardCharsets.UTF_8));
        EvalDataset dataset = new JsonlDatasetLoader().load(is);

        assertThat(dataset.size()).isEqualTo(2);
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String jsonl = "not valid json\n";
        var is = new ByteArrayInputStream(jsonl.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new JsonlDatasetLoader().load(is))
                .isInstanceOf(DatasetException.class)
                .hasMessageContaining("line 1");
    }
}
