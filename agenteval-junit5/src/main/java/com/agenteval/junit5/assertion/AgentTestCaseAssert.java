package com.agenteval.junit5.assertion;

import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;

import java.util.Objects;

/**
 * Fluent assertion chain for {@link AgentTestCase}.
 *
 * <p>Each assertion method evaluates immediately and throws {@link AssertionError}
 * on failure, allowing standard JUnit 5 failure reporting.</p>
 */
public final class AgentTestCaseAssert {

    private final AgentTestCase testCase;

    AgentTestCaseAssert(AgentTestCase testCase) {
        this.testCase = Objects.requireNonNull(testCase, "testCase must not be null");
    }

    /**
     * Evaluates the metric against the test case and asserts it passes.
     */
    public AgentTestCaseAssert meetsMetric(EvalMetric metric) {
        EvalScore score = metric.evaluate(testCase);
        if (!score.passed()) {
            throw new AssertionError(String.format(
                    "Metric '%s' failed: %.3f < %.3f (%s)",
                    metric.name(), score.value(), score.threshold(), score.reason()));
        }
        return this;
    }

    /**
     * Asserts that the test case has at least one tool call.
     */
    public AgentTestCaseAssert hasToolCalls() {
        if (testCase.getToolCalls().isEmpty()) {
            throw new AssertionError("Expected tool calls but found none");
        }
        return this;
    }

    /**
     * Asserts that a tool with the given name was called.
     */
    public AgentTestCaseAssert calledTool(String toolName) {
        boolean found = testCase.getToolCalls().stream()
                .anyMatch(tc -> tc.name().equals(toolName));
        if (!found) {
            throw new AssertionError("Expected tool '" + toolName
                    + "' to be called, but it was not. Actual tools: "
                    + testCase.getToolCalls().stream()
                    .map(tc -> tc.name())
                    .toList());
        }
        return this;
    }

    /**
     * Asserts that a tool with the given name was never called.
     */
    public AgentTestCaseAssert neverCalledTool(String toolName) {
        boolean found = testCase.getToolCalls().stream()
                .anyMatch(tc -> tc.name().equals(toolName));
        if (found) {
            throw new AssertionError("Expected tool '" + toolName
                    + "' to NOT be called, but it was");
        }
        return this;
    }

    /**
     * Asserts that the actual output contains the given substring.
     */
    public AgentTestCaseAssert outputContains(String substring) {
        String actual = testCase.getActualOutput();
        if (actual == null || !actual.contains(substring)) {
            throw new AssertionError("Expected actual output to contain '"
                    + substring + "', but was: " + actual);
        }
        return this;
    }

    /**
     * Asserts that the actual output does not contain the given substring.
     */
    public AgentTestCaseAssert outputDoesNotContain(String substring) {
        String actual = testCase.getActualOutput();
        if (actual != null && actual.contains(substring)) {
            throw new AssertionError("Expected actual output to NOT contain '"
                    + substring + "'");
        }
        return this;
    }

    /**
     * Asserts that the actual output is not null and not empty.
     */
    public AgentTestCaseAssert hasOutput() {
        String actual = testCase.getActualOutput();
        if (actual == null || actual.isEmpty()) {
            throw new AssertionError("Expected non-empty actual output, but was: " + actual);
        }
        return this;
    }

    /**
     * Returns the underlying test case for further inspection.
     */
    public AgentTestCase testCase() {
        return testCase;
    }
}
