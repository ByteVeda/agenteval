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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

/**
 * Generates a self-contained single-file HTML evaluation report.
 *
 * <p>The report embeds all CSS and JavaScript inline. Data is injected as a JSON blob
 * in a {@code <script>} tag, and client-side JavaScript renders the dashboard.</p>
 */
public final class HtmlReporter implements EvalReporter {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlReporter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final String TEMPLATE_PATH =
            "com/agenteval/reporting/html/report-template.html";

    private final HtmlReportConfig config;

    public HtmlReporter(HtmlReportConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    @Override
    public void report(EvalResult result) {
        LOG.debug("Writing HTML report to {}", config.outputPath());

        String template = loadTemplate();
        String jsonData = buildJsonData(result);
        String html = template
                .replace("{{TITLE}}", escapeHtml(config.title()))
                .replace("{{ DATA }}", jsonData);

        try {
            Files.writeString(config.outputPath(), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReportException(
                    "Failed to write HTML report to " + config.outputPath(), e);
        }
    }

    private String loadTemplate() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new ReportException(
                        "HTML report template not found: " + TEMPLATE_PATH);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReportException("Failed to load HTML report template", e);
        }
    }

    String buildJsonData(EvalResult result) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("title", config.title());
            root.put("averageScore", result.averageScore());
            root.put("passRate", result.passRate());
            root.put("totalCases", result.caseResults().size());
            root.put("failedCases", result.failedCases().size());
            root.put("durationMs", result.durationMs());

            ObjectNode metricAverages = root.putObject("metricAverages");
            result.averageScoresByMetric().forEach(metricAverages::put);

            if (config.includeDetails()) {
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
            }

            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new ReportException("Failed to build JSON data for HTML report", e);
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
