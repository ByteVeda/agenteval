package org.byteveda.agenteval.reporting.snapshot;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.model.EvalScore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot representation of a single test case result.
 *
 * @param input        the test case input
 * @param actualOutput the agent's actual output
 * @param passed       whether the case passed all metrics
 * @param scores       per-metric score data
 */
public record SnapshotCaseData(
        String input,
        String actualOutput,
        boolean passed,
        Map<String, SnapshotScoreData> scores
) {
    public SnapshotCaseData {
        Objects.requireNonNull(input, "input must not be null");
        scores = scores == null ? Map.of() : Map.copyOf(scores);
    }

    /**
     * Creates a snapshot case from an evaluation case result.
     */
    public static SnapshotCaseData from(CaseResult caseResult) {
        Objects.requireNonNull(caseResult, "caseResult must not be null");

        Map<String, SnapshotScoreData> scoreMap = new LinkedHashMap<>();
        for (Map.Entry<String, EvalScore> entry : caseResult.scores().entrySet()) {
            EvalScore s = entry.getValue();
            scoreMap.put(entry.getKey(),
                    new SnapshotScoreData(s.value(), s.threshold(), s.passed(), s.reason()));
        }

        return new SnapshotCaseData(
                caseResult.testCase().getInput(),
                caseResult.testCase().getActualOutput(),
                caseResult.passed(),
                scoreMap);
    }
}
