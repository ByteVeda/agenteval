package com.agenteval.datasets.json;

import com.agenteval.datasets.DatasetException;
import com.agenteval.datasets.EvalDataset;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonDatasetLoaderTest {

    private final JsonDatasetLoader loader = new JsonDatasetLoader();

    @Test
    void shouldLoadEnvelopeFormat() {
        Path path = Path.of("src/test/resources/test-dataset.json");
        EvalDataset dataset = loader.load(path);

        assertThat(dataset.getName()).isEqualTo("golden-set");
        assertThat(dataset.getVersion()).isEqualTo("1.0");
        assertThat(dataset.getTestCases()).hasSize(2);
        assertThat(dataset.getTestCases().get(0).getInput()).isEqualTo("How do I get a refund?");
        assertThat(dataset.getMetadata()).containsEntry("source", "manual");
    }

    @Test
    void shouldLoadBareArrayFormat() {
        Path path = Path.of("src/test/resources/test-dataset-bare.json");
        EvalDataset dataset = loader.load(path);

        assertThat(dataset.getName()).isNull();
        assertThat(dataset.getTestCases()).hasSize(2);
        assertThat(dataset.getTestCases().get(1).getInput()).isEqualTo("What are your business hours?");
    }

    @Test
    void shouldLoadEnvelopeFromInputStream() {
        String json = """
                {
                  "name": "stream-set",
                  "testCases": [
                    {"input": "hello", "expectedOutput": "world"}
                  ]
                }
                """;
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        EvalDataset dataset = loader.load(is);

        assertThat(dataset.getName()).isEqualTo("stream-set");
        assertThat(dataset.getTestCases()).hasSize(1);
    }

    @Test
    void shouldLoadBareArrayFromInputStream() {
        String json = """
                [
                  {"input": "q1", "expectedOutput": "a1"},
                  {"input": "q2", "expectedOutput": "a2"}
                ]
                """;
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        EvalDataset dataset = loader.load(is);

        assertThat(dataset.getName()).isNull();
        assertThat(dataset.getTestCases()).hasSize(2);
    }

    @Test
    void shouldAutoDetectWithLeadingWhitespace() {
        String json = "   \n  [{\"input\": \"q\"}]";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        EvalDataset dataset = loader.load(is);

        assertThat(dataset.getTestCases()).hasSize(1);
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String json = "not json";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> loader.load(is))
                .isInstanceOf(DatasetException.class);
    }

    @Test
    void shouldThrowOnNonExistentFile() {
        assertThatThrownBy(() -> loader.load(Path.of("nonexistent.json")))
                .isInstanceOf(DatasetException.class);
    }
}
