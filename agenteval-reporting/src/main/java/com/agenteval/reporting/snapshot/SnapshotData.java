package com.agenteval.reporting.snapshot;

import com.agenteval.core.eval.CaseResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Complete snapshot of an evaluation run, persisted as JSON.
 *
 * @param snapshotName   the logical name of this snapshot
 * @param createdAt      when the snapshot was created
 * @param averageScore   overall average score
 * @param passRate       overall pass rate (0.0–1.0)
 * @param totalCases     total number of test cases
 * @param durationMs     evaluation duration in milliseconds
 * @param metricAverages per-metric average scores
 * @param caseResults    per-case snapshot data
 */
public record SnapshotData(
        String snapshotName,
        Instant createdAt,
        double averageScore,
        double passRate,
        int totalCases,
        long durationMs,
        Map<String, Double> metricAverages,
        List<SnapshotCaseData> caseResults
) {
    public SnapshotData {
        Objects.requireNonNull(snapshotName, "snapshotName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        metricAverages = metricAverages == null ? Map.of() : Map.copyOf(metricAverages);
        caseResults = caseResults == null ? List.of() : List.copyOf(caseResults);
    }

    /**
     * Creates a snapshot from an evaluation result.
     *
     * @param name   the snapshot name
     * @param result the evaluation result
     * @return the snapshot data
     */
    public static SnapshotData from(String name, EvalResult result) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(result, "result must not be null");

        List<SnapshotCaseData> cases = result.caseResults().stream()
                .map(SnapshotCaseData::from)
                .toList();

        return new SnapshotData(
                name,
                Instant.now(),
                result.averageScore(),
                result.passRate(),
                result.caseResults().size(),
                result.durationMs(),
                new LinkedHashMap<>(result.averageScoresByMetric()),
                cases);
    }

    /**
     * Reconstructs a synthetic {@link EvalResult} for use with
     * {@link com.agenteval.reporting.regression.RegressionComparison}.
     */
    public EvalResult toEvalResult() {
        List<CaseResult> cases = new ArrayList<>(caseResults.size());

        for (SnapshotCaseData snapCase : caseResults) {
            AgentTestCase testCase = AgentTestCase.builder()
                    .input(snapCase.input())
                    .actualOutput(snapCase.actualOutput())
                    .build();

            Map<String, EvalScore> scores = new LinkedHashMap<>();
            for (Map.Entry<String, SnapshotScoreData> entry : snapCase.scores().entrySet()) {
                SnapshotScoreData sd = entry.getValue();
                scores.put(entry.getKey(),
                        new EvalScore(sd.value(), sd.threshold(), sd.passed(),
                                sd.reason() != null ? sd.reason() : "", entry.getKey()));
            }

            cases.add(new CaseResult(testCase, scores, snapCase.passed()));
        }

        return EvalResult.of(cases, durationMs);
    }
}
