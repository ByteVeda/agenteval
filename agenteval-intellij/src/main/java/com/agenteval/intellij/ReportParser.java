package com.agenteval.intellij;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses AgentEval JSON reports into {@link ReportModel} instances.
 */
public final class ReportParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReportParser() {}

    /**
     * Parses a JSON string into a report model.
     *
     * @param json the JSON string
     * @return the parsed report model
     * @throws IOException if parsing fails
     */
    public static ReportModel parse(String json) throws IOException {
        return MAPPER.readValue(json, ReportModel.class);
    }

    /**
     * Parses a JSON report file.
     *
     * @param path the path to the JSON file
     * @return the parsed report model
     * @throws IOException if reading or parsing fails
     */
    public static ReportModel parseFile(Path path) throws IOException {
        String json = Files.readString(path);
        return parse(json);
    }

    /**
     * Extracts per-metric pass/fail status from a report.
     *
     * <p>A metric is considered passing if its average score is above the
     * average threshold across all cases for that metric.</p>
     *
     * @param report the report model
     * @return map of metric name to pass/fail status
     */
    public static Map<String, Boolean> extractMetricPassFail(ReportModel report) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        if (report.getCaseResults() == null || report.getCaseResults().isEmpty()) {
            return result;
        }

        // Aggregate pass/fail per metric: metric passes if all cases pass that metric
        Map<String, Boolean> allPassed = new LinkedHashMap<>();
        for (ReportModel.CaseResultModel cr : report.getCaseResults()) {
            if (cr.getScores() == null) continue;
            for (Map.Entry<String, ReportModel.ScoreModel> entry : cr.getScores().entrySet()) {
                allPassed.merge(entry.getKey(), entry.getValue().isPassed(),
                        (existing, current) -> existing && current);
            }
        }
        return allPassed;
    }
}
