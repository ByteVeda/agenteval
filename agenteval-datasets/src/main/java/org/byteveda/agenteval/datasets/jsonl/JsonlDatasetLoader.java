package org.byteveda.agenteval.datasets.jsonl;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.DatasetException;
import org.byteveda.agenteval.datasets.DatasetLoader;
import org.byteveda.agenteval.datasets.EvalDataset;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads evaluation datasets from JSONL (JSON Lines) files.
 *
 * <p>Each line contains a single JSON object representing an {@link AgentTestCase}.</p>
 */
public final class JsonlDatasetLoader implements DatasetLoader {

    private static final Logger LOG = LoggerFactory.getLogger(JsonlDatasetLoader.class);

    private final ObjectMapper mapper;

    public JsonlDatasetLoader() {
        this(new ObjectMapper());
    }

    public JsonlDatasetLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvalDataset load(Path path) {
        LOG.debug("Loading JSONL dataset from {}", path);
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            throw new DatasetException("Failed to load JSONL dataset from " + path, e);
        }
    }

    @Override
    public EvalDataset load(InputStream inputStream) {
        LOG.debug("Loading JSONL dataset from input stream");
        try (var reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return parse(reader);
        } catch (IOException e) {
            throw new DatasetException("Failed to load JSONL dataset from input stream", e);
        }
    }

    private EvalDataset parse(BufferedReader reader) throws IOException {
        List<AgentTestCase> testCases = new ArrayList<>();
        String line;
        int lineNum = 0;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (line.isBlank()) continue;
            try {
                AgentTestCase tc = mapper.readValue(line, AgentTestCase.class);
                testCases.add(tc);
            } catch (Exception e) {
                throw new DatasetException(
                        "Failed to parse JSONL line " + lineNum + ": " + e.getMessage(), e);
            }
        }
        LOG.debug("Loaded {} test cases from JSONL", testCases.size());
        return EvalDataset.builder().testCases(testCases).build();
    }
}
