package org.byteveda.agenteval.datasets.csv;

import org.byteveda.agenteval.datasets.EvalDataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvDatasetLoaderTest {

    @Test
    void shouldLoadFromClasspath() {
        var is = getClass().getClassLoader().getResourceAsStream("test-dataset.csv");
        EvalDataset dataset = new CsvDatasetLoader().load(is);

        assertThat(dataset.size()).isEqualTo(2);
        assertThat(dataset.getTestCases().get(0).getInput()).isEqualTo("What is Java?");
        assertThat(dataset.getTestCases().get(0).getRetrievalContext()).hasSize(2);
    }

    @Test
    void shouldLoadFromPath(@TempDir Path tmpDir) throws Exception {
        Path csvFile = tmpDir.resolve("test.csv");
        Files.writeString(csvFile, "input,actualOutput\nHello,World\n");

        EvalDataset dataset = new CsvDatasetLoader().load(csvFile);

        assertThat(dataset.size()).isEqualTo(1);
        assertThat(dataset.getTestCases().get(0).getInput()).isEqualTo("Hello");
        assertThat(dataset.getTestCases().get(0).getActualOutput()).isEqualTo("World");
    }

    @Test
    void shouldHandleQuotedFields() {
        String csv = "input,actualOutput\n\"Hello, World\",\"He said \"\"hi\"\"\"\n";
        var is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        EvalDataset dataset = new CsvDatasetLoader().load(is);

        assertThat(dataset.getTestCases().get(0).getInput()).isEqualTo("Hello, World");
        assertThat(dataset.getTestCases().get(0).getActualOutput()).isEqualTo("He said \"hi\"");
    }

    @Test
    void shouldRejectEmptyFile() {
        var is = new ByteArrayInputStream(new byte[0]);
        assertThatThrownBy(() -> new CsvDatasetLoader().load(is))
                .hasMessageContaining("empty");
    }

    @Test
    void shouldParsePipeSeparatedLists() {
        String csv = "input,retrievalContext\nquery,doc1|doc2|doc3\n";
        var is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        EvalDataset dataset = new CsvDatasetLoader().load(is);

        assertThat(dataset.getTestCases().get(0).getRetrievalContext())
                .containsExactly("doc1", "doc2", "doc3");
    }
}
