package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.ToolCall;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic metric that measures whether the agent selected the correct tools.
 *
 * <p>Compares actual tool calls against expected tool calls by name.
 * <ul>
 *   <li><b>Unordered (default):</b> F1 score (harmonic mean of precision and recall)</li>
 *   <li><b>Ordered:</b> Longest common subsequence ratio</li>
 * </ul>
 */
public final class ToolSelectionAccuracyMetric implements EvalMetric {

    private static final String NAME = "ToolSelectionAccuracy";
    private static final double DEFAULT_THRESHOLD = 0.8;

    private final double threshold;
    private final boolean orderMatters;

    public ToolSelectionAccuracyMetric() {
        this(DEFAULT_THRESHOLD, false);
    }

    public ToolSelectionAccuracyMetric(double threshold) {
        this(threshold, false);
    }

    public ToolSelectionAccuracyMetric(double threshold, boolean orderMatters) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        this.threshold = threshold;
        this.orderMatters = orderMatters;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        List<String> actual = testCase.getToolCalls().stream()
                .map(ToolCall::name)
                .toList();
        List<String> expected = testCase.getExpectedToolCalls().stream()
                .map(ToolCall::name)
                .toList();

        if (expected.isEmpty() && actual.isEmpty()) {
            return EvalScore.of(1.0, threshold, "No tools expected or called");
        }
        if (expected.isEmpty()) {
            return EvalScore.of(0.0, threshold,
                    "No tools expected but " + actual.size() + " were called");
        }
        if (actual.isEmpty()) {
            return EvalScore.of(0.0, threshold,
                    "Expected " + expected.size() + " tools but none were called");
        }

        double score;
        String reason;

        if (orderMatters) {
            int lcsLength = longestCommonSubsequence(actual, expected);
            score = (double) lcsLength / Math.max(actual.size(), expected.size());
            reason = String.format("LCS %d / max(%d, %d) = %.2f",
                    lcsLength, actual.size(), expected.size(), score);
        } else {
            Set<String> actualSet = new HashSet<>(actual);
            Set<String> expectedSet = new HashSet<>(expected);

            Set<String> truePositives = new HashSet<>(actualSet);
            truePositives.retainAll(expectedSet);
            int tp = truePositives.size();

            double precision = (double) tp / actualSet.size();
            double recall = (double) tp / expectedSet.size();

            if (precision + recall == 0) {
                score = 0.0;
            } else {
                score = 2 * precision * recall / (precision + recall);
            }

            reason = String.format(
                    "F1=%.2f (precision=%.2f, recall=%.2f) — matched: %s",
                    score, precision, recall,
                    truePositives.stream().sorted().collect(Collectors.joining(", ")));
        }

        score = Math.min(1.0, Math.max(0.0, score));
        return EvalScore.of(score, threshold, reason);
    }

    @Override
    public String name() {
        return NAME;
    }

    static int longestCommonSubsequence(List<String> a, List<String> b) {
        int m = a.size();
        int n = b.size();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }
}
