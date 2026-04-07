package org.byteveda.agenteval.contracts;

/**
 * Severity level for contract violations.
 */
public enum ContractSeverity {
    /** Informational — violation is logged but does not fail the test. */
    WARNING,
    /** Standard — violation fails the test (default). */
    ERROR,
    /** Critical — violation fails the test and stops further contract checks. */
    CRITICAL
}
