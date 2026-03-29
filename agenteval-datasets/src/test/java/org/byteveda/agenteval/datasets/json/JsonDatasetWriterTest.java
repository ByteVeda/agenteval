package org.byteveda.agenteval.datasets.json;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonDatasetWriterTest {

    private final JsonDatasetWriter writer = new JsonDatasetWriter();
    private final JsonDatasetLoader loader = new JsonDatasetLoader();

    @Test
    void shouldWriteToOutputStream() {
        var dataset = EvalDataset.builder()
                .name("test-write")
                .version("1.0")
                .testCases(List.of(
                        AgentTestCase.builder().input("q1").expectedOutput("a1").build()
                ))
                .metadata(Map.of("key", "value"))
                .build();

        var out = new ByteArrayOutputStream();
        writer.write(dataset, out);

        String json = out.toString();
        assertThat(json).contains("\"name\" : \"test-write\"");
        assertThat(json).contains("\"input\" : \"q1\"");
    }

    @Test
    void shouldRoundTripThroughFile(@TempDir Path tempDir) {
        var original = EvalDataset.builder()
                .name("round-trip")
                .version("2.0")
                .testCases(List.of(
                        AgentTestCase.builder()
                                .input("How do I reset my password?")
                                .expectedOutput("Go to Settings > Security.")
                                .build(),
                        AgentTestCase.builder()
                                .input("Where is my order?")
                                .expectedOutput("Check your order status page.")
                                .build()
                ))
                .build();

        Path file = tempDir.resolve("dataset.json");
        writer.write(original, file);
        EvalDataset loaded = loader.load(file);

        assertThat(loaded.getName()).isEqualTo("round-trip");
        assertThat(loaded.getVersion()).isEqualTo("2.0");
        assertThat(loaded.getTestCases()).hasSize(2);
        assertThat(loaded.getTestCases().get(0).getInput())
                .isEqualTo("How do I reset my password?");
    }

    @Test
    void shouldWritePrettyPrinted() {
        var dataset = EvalDataset.builder()
                .name("pretty")
                .testCases(List.of(AgentTestCase.builder().input("q").build()))
                .build();

        var out = new ByteArrayOutputStream();
        writer.write(dataset, out);

        String json = out.toString();
        assertThat(json).contains("\n");
        assertThat(json.lines().count()).isGreaterThan(1);
    }
}
