package com.agenteval.datasets.json;

import com.agenteval.datasets.DatasetException;
import com.agenteval.datasets.DatasetWriter;
import com.agenteval.datasets.EvalDataset;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes evaluation datasets to JSON files in envelope format with pretty-printing.
 */
public final class JsonDatasetWriter implements DatasetWriter {

    private static final Logger LOG = LoggerFactory.getLogger(JsonDatasetWriter.class);

    private final ObjectMapper mapper;

    public JsonDatasetWriter() {
        this(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT));
    }

    public JsonDatasetWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void write(EvalDataset dataset, Path path) {
        LOG.debug("Writing dataset '{}' to {}", dataset.getName(), path);
        try (OutputStream out = Files.newOutputStream(path)) {
            write(dataset, out);
        } catch (IOException e) {
            throw new DatasetException("Failed to write dataset to " + path, e);
        }
    }

    @Override
    public void write(EvalDataset dataset, OutputStream outputStream) {
        try {
            mapper.writeValue(outputStream, dataset);
        } catch (IOException e) {
            throw new DatasetException("Failed to write dataset to output stream", e);
        }
    }
}
