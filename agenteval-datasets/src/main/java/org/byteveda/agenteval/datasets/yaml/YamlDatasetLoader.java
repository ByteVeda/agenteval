package org.byteveda.agenteval.datasets.yaml;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.DatasetException;
import org.byteveda.agenteval.datasets.DatasetLoader;
import org.byteveda.agenteval.datasets.EvalDataset;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads evaluation datasets from YAML files (read-only).
 *
 * <p>Supports two formats:</p>
 * <ul>
 *   <li><strong>Envelope format:</strong> top-level object with {@code name}, {@code testCases} keys</li>
 *   <li><strong>Bare list format:</strong> a YAML sequence of test case objects</li>
 * </ul>
 *
 * <pre>{@code
 * EvalDataset dataset = new YamlDatasetLoader().load(Path.of("golden-set.yaml"));
 * }</pre>
 */
public final class YamlDatasetLoader implements DatasetLoader {

    private static final Logger LOG = LoggerFactory.getLogger(YamlDatasetLoader.class);
    private static final TypeReference<List<AgentTestCase>> TEST_CASE_LIST =
            new TypeReference<>() { };

    private final ObjectMapper yamlMapper;

    public YamlDatasetLoader() {
        this(new ObjectMapper(new YAMLFactory()));
    }

    public YamlDatasetLoader(ObjectMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    @Override
    public EvalDataset load(Path path) {
        LOG.debug("Loading YAML dataset from {}", path);
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        } catch (IOException e) {
            throw new DatasetException("Failed to load YAML dataset from " + path, e);
        }
    }

    @Override
    public EvalDataset load(InputStream inputStream) {
        LOG.debug("Loading YAML dataset from input stream");
        try {
            byte[] bytes = inputStream.readAllBytes();
            return parse(bytes);
        } catch (IOException e) {
            throw new DatasetException("Failed to load YAML dataset from input stream", e);
        }
    }

    private EvalDataset parse(byte[] bytes) throws IOException {
        // Try envelope format first (object with name/testCases)
        try {
            EvalDataset dataset = yamlMapper.readValue(bytes, EvalDataset.class);
            if (dataset.getTestCases() != null && !dataset.getTestCases().isEmpty()) {
                LOG.debug("Detected envelope format");
                return dataset;
            }
        } catch (IOException ignored) {
            // fall through to bare list
        }

        // Try bare list format
        LOG.debug("Detected bare list format");
        List<AgentTestCase> testCases = yamlMapper.readValue(bytes, TEST_CASE_LIST);
        return EvalDataset.builder()
                .testCases(testCases)
                .build();
    }
}
