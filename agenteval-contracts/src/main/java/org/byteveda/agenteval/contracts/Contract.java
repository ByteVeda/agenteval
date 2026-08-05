package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.model.AgentTestCase;

/**
 * A behavioral invariant that an AI agent must satisfy.
 *
 * <p>Unlike {@link org.byteveda.agenteval.core.metric.EvalMetric} which scores quality
 * on a 0.0–1.0 spectrum, contracts are binary: the agent either satisfies the invariant
 * or it doesn't. A single violation means the contract is broken.</p>
 *
 * <p>Three implementations are provided:</p>
 * <ul>
 *   <li>{@link DeterministicContract} — fast, predicate-based checks (regex, contains, tool calls)</li>
 *   <li>{@link LLMJudgedContract} — semantic checks via LLM-as-judge</li>
 *   <li>{@link CompositeContract} — groups multiple contracts into a suite</li>
 * </ul>
 *
 * @see Contracts
 * @see ContractVerifier
 */
public sealed interface Contract
        permits DeterministicContract, LLMJudgedContract, CompositeContract {

    /**
     * Returns the unique name of this contract.
     */
    String name();

    /**
     * Returns a human-readable description of what this contract enforces.
     */
    String description();

    /**
     * Returns the severity level for violations of this contract.
     */
    ContractSeverity severity();

    /**
     * Returns the category of this contract.
     */
    ContractType type();

    /**
     * Checks this contract against a single test case.
     *
     * @param testCase the test case to verify
     * @return a verdict indicating pass or violation with evidence
     */
    ContractVerdict check(AgentTestCase testCase);
}
