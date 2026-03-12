package com.agenteval.datasets.jsonl;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.datasets.EvalDataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonlDatasetWriterTest {

    @Test
    void shouldWriteOneJsonPerLine() {
        var dataset = EvalDataset.builder()
                .testCases(List.of(
                        AgentTestCase.builder().input("q1").build(),
                        AgentTestCase.builder().input("q2").build()))
                .build();

        var out = new ByteArrayOutputStream();
        new JsonlDatasetWriter().write(dataset, out);

        String[] lines = out.toString().trim().split("\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).contains("\"input\":\"q1\"");
        assertThat(lines[1]).contains("\"input\":\"q2\"");
    }

    @Test
    void shouldWriteToFile(@TempDir Path tmpDir) {
        Path jsonlFile = tmpDir.resolve("out.jsonl");
        var dataset = EvalDataset.builder()
                .testCases(List.of(
                        AgentTestCase.builder().input("q").build()))
                .build();

        new JsonlDatasetWriter().write(dataset, jsonlFile);

        assertThat(jsonlFile).exists();
    }
}
