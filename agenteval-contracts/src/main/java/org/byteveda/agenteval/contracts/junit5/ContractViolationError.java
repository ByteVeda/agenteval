package org.byteveda.agenteval.contracts.junit5;

import org.byteveda.agenteval.contracts.ContractViolation;

import java.util.List;

/**
 * Custom assertion error thrown when contract violations are detected during a JUnit test.
 */
public class ContractViolationError extends AssertionError {

    private static final long serialVersionUID = 1L;

    private final transient List<ContractViolation> violations;

    public ContractViolationError(String message, List<ContractViolation> violations) {
        super(message);
        this.violations = List.copyOf(violations);
    }

    /**
     * Returns the contract violations that caused this error.
     */
    public List<ContractViolation> violations() {
        return violations;
    }
}
