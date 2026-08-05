package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.ToolCall;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic metric that measures whether tool arguments match expected values.
 *
 * <p>Matches actual tool calls to expected tool calls by name, then deep-compares
 * argument maps. In strict mode, extra arguments count as failures.</p>
 */
public final class ToolArgumentCorrectnessMetric implements EvalMetric {

    private static final String NAME = "ToolArgumentCorrectness";
    private static final double DEFAULT_THRESHOLD = 0.8;

    private final double threshold;
    private final boolean strictMode;

    public ToolArgumentCorrectnessMetric() {
        this(DEFAULT_THRESHOLD, false);
    }

    public ToolArgumentCorrectnessMetric(double threshold) {
        this(threshold, false);
    }

    public ToolArgumentCorrectnessMetric(double threshold, boolean strictMode) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        this.threshold = threshold;
        this.strictMode = strictMode;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        List<ToolCall> actual = testCase.getToolCalls();
        List<ToolCall> expected = testCase.getExpectedToolCalls();

        if (expected.isEmpty() && actual.isEmpty()) {
            return EvalScore.of(1.0, threshold, "No tools expected or called");
        }
        if (expected.isEmpty()) {
            return EvalScore.of(0.0, threshold,
                    "No expected tool calls to compare against");
        }
        if (actual.isEmpty()) {
            return EvalScore.of(0.0, threshold,
                    "Expected " + expected.size() + " tool calls but none were made");
        }

        int totalArgs = 0;
        int correctArgs = 0;

        for (ToolCall expectedTc : expected) {
            ToolCall matchedActual = findByName(actual, expectedTc.name());
            if (matchedActual == null) {
                totalArgs += expectedTc.arguments().size();
                continue;
            }

            Map<String, Object> expectedArgs = expectedTc.arguments();
            Map<String, Object> actualArgs = matchedActual.arguments();

            for (Map.Entry<String, Object> entry : expectedArgs.entrySet()) {
                totalArgs++;
                Object actualVal = actualArgs.get(entry.getKey());
                if (Objects.equals(entry.getValue(), actualVal)) {
                    correctArgs++;
                }
            }

            if (strictMode) {
                for (String key : actualArgs.keySet()) {
                    if (!expectedArgs.containsKey(key)) {
                        totalArgs++;
                    }
                }
            }
        }

        if (totalArgs == 0) {
            return EvalScore.of(1.0, threshold, "No arguments to compare");
        }

        double score = Math.min(1.0, Math.max(0.0, (double) correctArgs / totalArgs));
        String reason = String.format("Correct args: %d / %d = %.2f%s",
                correctArgs, totalArgs, score,
                strictMode ? " (strict mode)" : "");
        return EvalScore.of(score, threshold, reason);
    }

    @Override
    public String name() {
        return NAME;
    }

    private static ToolCall findByName(List<ToolCall> calls, String name) {
        for (ToolCall tc : calls) {
            if (tc.name().equals(name)) {
                return tc;
            }
        }
        return null;
    }
}
