package com.agenteval.datasets.jsonl;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.datasets.DatasetException;
import com.agenteval.datasets.DatasetWriter;
import com.agenteval.datasets.EvalDataset;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes evaluation datasets to JSONL (JSON Lines) files.
 *
 * <p>Each line contains a single JSON object representing an {@link AgentTestCase}.</p>
 */
public final class JsonlDatasetWriter implements DatasetWriter {

    private static final Logger LOG = LoggerFactory.getLogger(JsonlDatasetWriter.class);

    private final ObjectMapper mapper;

    public JsonlDatasetWriter() {
        this(new ObjectMapper());
    }

    public JsonlDatasetWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void write(EvalDataset dataset, Path path) {
        LOG.debug("Writing JSONL dataset to {}", path);
        try (var out = Files.newOutputStream(path)) {
            write(dataset, out);
        } catch (IOException e) {
            throw new DatasetException("Failed to write JSONL dataset to " + path, e);
        }
    }

    @Override
    public void write(EvalDataset dataset, OutputStream outputStream) {
        try {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            for (AgentTestCase tc : dataset.getTestCases()) {
                writer.write(mapper.writeValueAsString(tc));
                writer.write('\n');
            }
            writer.flush();
        } catch (IOException e) {
            throw new DatasetException("Failed to write JSONL dataset", e);
        }
    }
}
