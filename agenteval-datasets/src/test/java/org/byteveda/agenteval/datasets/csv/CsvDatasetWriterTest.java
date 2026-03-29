package org.byteveda.agenteval.datasets.csv;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvDatasetWriterTest {

    @Test
    void shouldWriteToOutputStream() {
        var dataset = EvalDataset.builder()
                .testCases(List.of(
                        AgentTestCase.builder()
                                .input("Hello")
                                .actualOutput("World")
                                .build()))
                .build();

        var out = new ByteArrayOutputStream();
        new CsvDatasetWriter().write(dataset, out);

        String csv = out.toString();
        assertThat(csv).contains("input,actualOutput,expectedOutput,retrievalContext,context");
        assertThat(csv).contains("Hello,World");
    }

    @Test
    void shouldWriteToFile(@TempDir Path tmpDir) {
        Path csvFile = tmpDir.resolve("out.csv");
        var dataset = EvalDataset.builder()
                .testCases(List.of(
                        AgentTestCase.builder()
                                .input("q")
                                .actualOutput("a")
                                .build()))
                .build();

        new CsvDatasetWriter().write(dataset, csvFile);

        assertThat(csvFile).exists();
    }

    @Test
    void shouldEscapeCommasAndQuotes() {
        assertThat(CsvDatasetWriter.escapeCsv("hello,world")).isEqualTo("\"hello,world\"");
        assertThat(CsvDatasetWriter.escapeCsv("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvDatasetWriter.escapeCsv("simple")).isEqualTo("simple");
        assertThat(CsvDatasetWriter.escapeCsv(null)).isEmpty();
    }

    @Test
    void shouldWritePipeSeparatedLists() {
        var dataset = EvalDataset.builder()
                .testCases(List.of(
                        AgentTestCase.builder()
                                .input("q")
                                .retrievalContext(List.of("doc1", "doc2"))
                                .build()))
                .build();

        var out = new ByteArrayOutputStream();
        new CsvDatasetWriter().write(dataset, out);

        assertThat(out.toString()).contains("doc1|doc2");
    }
}
