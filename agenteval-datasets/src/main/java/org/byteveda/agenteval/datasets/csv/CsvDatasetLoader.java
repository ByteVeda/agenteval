package org.byteveda.agenteval.datasets.csv;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.DatasetException;
import org.byteveda.agenteval.datasets.DatasetLoader;
import org.byteveda.agenteval.datasets.EvalDataset;
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
import java.util.Arrays;
import java.util.List;

/**
 * Loads evaluation datasets from RFC 4180 CSV files.
 *
 * <p>Expected columns: {@code input, actualOutput, expectedOutput, retrievalContext, context}.
 * List fields (retrievalContext, context) use pipe ({@code |}) as separator.</p>
 */
public final class CsvDatasetLoader implements DatasetLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CsvDatasetLoader.class);
    private static final String PIPE_SEPARATOR = "\\|";

    @Override
    public EvalDataset load(Path path) {
        LOG.debug("Loading CSV dataset from {}", path);
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            throw new DatasetException("Failed to load CSV dataset from " + path, e);
        }
    }

    @Override
    public EvalDataset load(InputStream inputStream) {
        LOG.debug("Loading CSV dataset from input stream");
        try (var reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return parse(reader);
        } catch (IOException e) {
            throw new DatasetException("Failed to load CSV dataset from input stream", e);
        }
    }

    private EvalDataset parse(BufferedReader reader) throws IOException {
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new DatasetException("CSV file is empty — expected header row");
        }

        String[] headers = parseCsvLine(headerLine);
        int inputIdx = indexOf(headers, "input");
        int actualOutputIdx = indexOf(headers, "actualOutput");
        int expectedOutputIdx = indexOf(headers, "expectedOutput");
        int retrievalContextIdx = indexOf(headers, "retrievalContext");
        int contextIdx = indexOf(headers, "context");

        if (inputIdx == -1) {
            throw new DatasetException("CSV header must contain 'input' column");
        }

        List<AgentTestCase> testCases = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;

            String[] fields = parseCsvLine(line);
            var builder = AgentTestCase.builder()
                    .input(getField(fields, inputIdx));

            if (actualOutputIdx >= 0) {
                builder.actualOutput(getField(fields, actualOutputIdx));
            }
            if (expectedOutputIdx >= 0) {
                builder.expectedOutput(getField(fields, expectedOutputIdx));
            }
            if (retrievalContextIdx >= 0) {
                String val = getField(fields, retrievalContextIdx);
                if (val != null && !val.isEmpty()) {
                    builder.retrievalContext(splitPipe(val));
                }
            }
            if (contextIdx >= 0) {
                String val = getField(fields, contextIdx);
                if (val != null && !val.isEmpty()) {
                    builder.context(splitPipe(val));
                }
            }

            testCases.add(builder.build());
        }

        LOG.debug("Loaded {} test cases from CSV", testCases.size());
        return EvalDataset.builder().testCases(testCases).build();
    }

    static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(String[]::new);
    }

    private static int indexOf(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private static String getField(String[] fields, int idx) {
        if (idx >= fields.length) return null;
        String val = fields[idx].trim();
        return val.isEmpty() ? null : val;
    }

    private static List<String> splitPipe(String value) {
        return Arrays.stream(value.split(PIPE_SEPARATOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
