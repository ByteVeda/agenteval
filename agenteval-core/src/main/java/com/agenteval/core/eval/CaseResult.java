package com.agenteval.core.eval;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluation result for a single test case across all metrics.
 */
public record CaseResult(
        AgentTestCase testCase,
        Map<String, EvalScore> scores,
        boolean passed
) {
    public CaseResult {
        Objects.requireNonNull(testCase, "testCase must not be null");
        scores = scores == null ? Map.of() : Map.copyOf(scores);
    }

    /**
     * Returns the list of scores that failed their thresholds.
     */
    public List<EvalScore> failedScores() {
        return scores.values().stream()
                .filter(s -> !s.passed())
                .toList();
    }

    /**
     * Returns the average score across all metrics for this case.
     */
    public double averageScore() {
        if (scores.isEmpty()) return 0.0;
        return scores.values().stream()
                .mapToDouble(EvalScore::value)
                .average()
                .orElse(0.0);
    }
}
