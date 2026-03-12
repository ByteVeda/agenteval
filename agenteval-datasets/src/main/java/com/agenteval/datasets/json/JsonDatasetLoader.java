package com.agenteval.datasets.json;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.datasets.DatasetException;
import com.agenteval.datasets.DatasetLoader;
import com.agenteval.datasets.EvalDataset;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads evaluation datasets from JSON files.
 *
 * <p>Supports two formats:</p>
 * <ul>
 *   <li><strong>Envelope format:</strong> {@code {"name":"...","testCases":[...]}}</li>
 *   <li><strong>Bare array format:</strong> {@code [{...}, ...]}</li>
 * </ul>
 *
 * <p>Auto-detects the format by inspecting the first non-whitespace character.</p>
 */
public final class JsonDatasetLoader implements DatasetLoader {

    private static final Logger LOG = LoggerFactory.getLogger(JsonDatasetLoader.class);
    private static final TypeReference<List<AgentTestCase>> TEST_CASE_LIST =
            new TypeReference<>() { };

    private final ObjectMapper mapper;

    public JsonDatasetLoader() {
        this(new ObjectMapper());
    }

    public JsonDatasetLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvalDataset load(Path path) {
        LOG.debug("Loading dataset from {}", path);
        try {
            byte[] bytes = Files.readAllBytes(path);
            return parse(bytes);
        } catch (IOException e) {
            throw new DatasetException("Failed to load dataset from " + path, e);
        }
    }

    @Override
    public EvalDataset load(InputStream inputStream) {
        LOG.debug("Loading dataset from input stream");
        try {
            byte[] bytes = inputStream.readAllBytes();
            return parse(bytes);
        } catch (IOException e) {
            throw new DatasetException("Failed to load dataset from input stream", e);
        }
    }

    private EvalDataset parse(byte[] bytes) throws IOException {
        if (isBareArray(bytes)) {
            LOG.debug("Detected bare array format");
            List<AgentTestCase> testCases = mapper.readValue(bytes, TEST_CASE_LIST);
            return EvalDataset.builder()
                    .testCases(testCases)
                    .build();
        }
        LOG.debug("Detected envelope format");
        return mapper.readValue(bytes, EvalDataset.class);
    }

    private static boolean isBareArray(byte[] bytes) {
        for (byte b : bytes) {
            if (!Character.isWhitespace(b)) {
                return b == '[';
            }
        }
        return false;
    }
}
