package org.byteveda.agenteval.contracts;

import java.util.Objects;

/**
 * Evidence of a single contract violation.
 */
public record ContractViolation(
        String contractName,
        String evidence,
        ContractSeverity severity
) {
    public ContractViolation {
        Objects.requireNonNull(contractName, "contractName must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
    }
}
