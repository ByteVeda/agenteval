package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;

/**
 * Sealed interface for chaos injection strategies.
 *
 * <p>Each implementation modifies an {@link AgentTestCase} to simulate
 * a specific failure mode, allowing evaluation of agent resilience.</p>
 */
public sealed interface ChaosInjector
        permits ToolFailureInjector, ContextCorruptionInjector,
        LatencyInjector, SchemaMutationInjector {

    /**
     * Injects chaos into the given test case, returning a modified copy.
     *
     * @param testCase the original test case
     * @return a new test case with chaos injected
     */
    AgentTestCase inject(AgentTestCase testCase);

    /**
     * Returns a human-readable description of this injector's behavior.
     */
    String description();
}
