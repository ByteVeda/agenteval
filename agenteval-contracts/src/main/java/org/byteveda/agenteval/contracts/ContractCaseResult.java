package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.model.AgentTestCase;

import java.util.List;
import java.util.Objects;

/**
 * Result of verifying all contracts against a single input.
 */
public record ContractCaseResult(
        AgentTestCase testCase,
        List<ContractVerdict> verdicts,
        boolean allPassed
) {
    public ContractCaseResult {
        Objects.requireNonNull(testCase, "testCase must not be null");
        verdicts = verdicts == null ? List.of() : List.copyOf(verdicts);
    }

    /**
     * Returns all violations across all contracts for this test case.
     */
    public List<ContractViolation> violations() {
        return verdicts.stream()
                .filter(v -> !v.passed())
                .flatMap(v -> v.violations().stream())
                .toList();
    }
}
