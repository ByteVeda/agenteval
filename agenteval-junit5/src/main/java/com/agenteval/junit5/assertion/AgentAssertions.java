package com.agenteval.junit5.assertion;

import com.agenteval.core.model.AgentTestCase;

/**
 * Static entry point for fluent agent test case assertions.
 *
 * <pre>{@code
 * AgentAssertions.assertThat(testCase)
 *     .meetsMetric(new AnswerRelevancyMetric(judge, 0.7))
 *     .hasToolCalls()
 *     .calledTool("SearchOrders")
 *     .neverCalledTool("DeleteOrder")
 *     .outputContains("refund");
 * }</pre>
 *
 * <p>Works independently of {@code @AgentTest} and the extension — usable
 * in any JUnit test.</p>
 */
public final class AgentAssertions {

    private AgentAssertions() {}

    /**
     * Begins a fluent assertion chain for the given test case.
     *
     * @param testCase the test case to assert against
     * @return a fluent assertion object
     */
    public static AgentTestCaseAssert assertThat(AgentTestCase testCase) {
        return new AgentTestCaseAssert(testCase);
    }
}
