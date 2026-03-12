package com.agenteval.reporting;

import com.agenteval.core.eval.CaseResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.core.model.EvalScore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Reports evaluation results as a JSON file.
 *
 * <p>Serializes the full {@link EvalResult} structure including per-case scores,
 * metric summaries, and aggregate statistics.</p>
 */
public final class JsonReporter implements EvalReporter {

    private static final Logger LOG = LoggerFactory.getLogger(JsonReporter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path outputPath;

    public JsonReporter(Path outputPath) {
        this.outputPath = Objects.requireNonNull(outputPath, "outputPath must not be null");
    }

    @Override
    public void report(EvalResult result) {
        LOG.debug("Writing JSON report to {}", outputPath);

        ObjectNode root = MAPPER.createObjectNode();
        root.put("averageScore", result.averageScore());
        root.put("passRate", result.passRate());
        root.put("totalCases", result.caseResults().size());
        root.put("failedCases", result.failedCases().size());
        root.put("durationMs", result.durationMs());

        ObjectNode metricAverages = root.putObject("metricAverages");
        result.averageScoresByMetric().forEach(metricAverages::put);

        ArrayNode cases = root.putArray("caseResults");
        for (CaseResult cr : result.caseResults()) {
            ObjectNode caseNode = cases.addObject();
            caseNode.put("input", cr.testCase().getInput());
            caseNode.put("passed", cr.passed());
            caseNode.put("averageScore", cr.averageScore());

            ObjectNode scores = caseNode.putObject("scores");
            for (Map.Entry<String, EvalScore> entry : cr.scores().entrySet()) {
                ObjectNode scoreNode = scores.putObject(entry.getKey());
                scoreNode.put("value", entry.getValue().value());
                scoreNode.put("threshold", entry.getValue().threshold());
                scoreNode.put("passed", entry.getValue().passed());
                scoreNode.put("reason", entry.getValue().reason());
            }
        }

        try (OutputStream out = Files.newOutputStream(outputPath)) {
            MAPPER.writeValue(out, root);
        } catch (IOException e) {
            throw new ReportException("Failed to write JSON report to " + outputPath, e);
        }
    }
}
