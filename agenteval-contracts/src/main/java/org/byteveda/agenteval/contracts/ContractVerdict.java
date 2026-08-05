package org.byteveda.agenteval.contracts;

import java.util.List;
import java.util.Objects;

/**
 * Result of checking a single contract against a single test case.
 */
public record ContractVerdict(
        String contractName,
        boolean passed,
        List<ContractViolation> violations
) {
    public ContractVerdict {
        Objects.requireNonNull(contractName, "contractName must not be null");
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    /**
     * Creates a passing verdict.
     */
    public static ContractVerdict pass(String contractName) {
        return new ContractVerdict(contractName, true, List.of());
    }

    /**
     * Creates a failing verdict with a single violation.
     */
    public static ContractVerdict violation(String contractName, String evidence,
                                            ContractSeverity severity) {
        return new ContractVerdict(contractName, false,
                List.of(new ContractViolation(contractName, evidence, severity)));
    }
}
