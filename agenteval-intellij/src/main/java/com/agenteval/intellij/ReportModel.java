package com.agenteval.intellij;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Lightweight model for parsing AgentEval JSON reports in the IntelliJ plugin.
 *
 * <p>Matches the output format of {@code JsonReporter}. No dependency on agenteval-core.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ReportModel {

    @JsonProperty("averageScore")
    private double averageScore;

    @JsonProperty("passRate")
    private double passRate;

    @JsonProperty("totalCases")
    private int totalCases;

    @JsonProperty("failedCases")
    private int failedCases;

    @JsonProperty("durationMs")
    private long durationMs;

    @JsonProperty("metricAverages")
    private Map<String, Double> metricAverages;

    @JsonProperty("caseResults")
    private List<CaseResultModel> caseResults;

    public ReportModel() {}

    public double getAverageScore() { return averageScore; }
    public double getPassRate() { return passRate; }
    public int getTotalCases() { return totalCases; }
    public int getFailedCases() { return failedCases; }
    public long getDurationMs() { return durationMs; }
    public Map<String, Double> getMetricAverages() { return metricAverages; }
    public List<CaseResultModel> getCaseResults() { return caseResults; }

    /**
     * Returns true if the overall evaluation passed (all cases passed).
     */
    public boolean isOverallPass() {
        return failedCases == 0;
    }

    /**
     * A single test case result within the report.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CaseResultModel {

        @JsonProperty("input")
        private String input;

        @JsonProperty("passed")
        private boolean passed;

        @JsonProperty("averageScore")
        private double averageScore;

        @JsonProperty("scores")
        private Map<String, ScoreModel> scores;

        public CaseResultModel() {}

        public String getInput() { return input; }
        public boolean isPassed() { return passed; }
        public double getAverageScore() { return averageScore; }
        public Map<String, ScoreModel> getScores() { return scores; }
    }

    /**
     * A metric score within a case result.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ScoreModel {

        @JsonProperty("value")
        private double value;

        @JsonProperty("threshold")
        private double threshold;

        @JsonProperty("passed")
        private boolean passed;

        @JsonProperty("reason")
        private String reason;

        public ScoreModel() {}

        public double getValue() { return value; }
        public double getThreshold() { return threshold; }
        public boolean isPassed() { return passed; }
        public String getReason() { return reason; }
    }
}
