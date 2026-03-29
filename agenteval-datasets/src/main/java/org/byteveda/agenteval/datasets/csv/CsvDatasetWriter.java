package org.byteveda.agenteval.datasets.csv;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.DatasetException;
import org.byteveda.agenteval.datasets.DatasetWriter;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes evaluation datasets to RFC 4180 CSV files.
 *
 * <p>List fields use pipe ({@code |}) as separator.</p>
 */
public final class CsvDatasetWriter implements DatasetWriter {

    private static final Logger LOG = LoggerFactory.getLogger(CsvDatasetWriter.class);
    private static final String HEADER = "input,actualOutput,expectedOutput,retrievalContext,context";

    @Override
    public void write(EvalDataset dataset, Path path) {
        LOG.debug("Writing CSV dataset to {}", path);
        try (var out = Files.newOutputStream(path)) {
            write(dataset, out);
        } catch (IOException e) {
            throw new DatasetException("Failed to write CSV dataset to " + path, e);
        }
    }

    @Override
    public void write(EvalDataset dataset, OutputStream outputStream) {
        try {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            writer.write(HEADER);
            writer.write('\n');

            for (AgentTestCase tc : dataset.getTestCases()) {
                writer.write(escapeCsv(tc.getInput()));
                writer.write(',');
                writer.write(escapeCsv(tc.getActualOutput()));
                writer.write(',');
                writer.write(escapeCsv(tc.getExpectedOutput()));
                writer.write(',');
                writer.write(escapeCsv(joinPipe(tc.getRetrievalContext())));
                writer.write(',');
                writer.write(escapeCsv(joinPipe(tc.getContext())));
                writer.write('\n');
            }

            writer.flush();
        } catch (IOException e) {
            throw new DatasetException("Failed to write CSV dataset", e);
        }
    }

    static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String joinPipe(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return String.join("|", values);
    }
}
