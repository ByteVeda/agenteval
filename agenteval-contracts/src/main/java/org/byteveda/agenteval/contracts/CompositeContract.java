package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.model.AgentTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A contract that groups multiple child contracts into a logical suite.
 * Passes only if ALL child contracts pass.
 */
public non-sealed class CompositeContract implements Contract {

    private final String name;
    private final String description;
    private final ContractSeverity severity;
    private final ContractType type;
    private final List<Contract> contracts;

    CompositeContract(String name, String description,
            ContractSeverity severity, ContractType type,
            List<Contract> contracts) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.severity = Objects.requireNonNull(severity);
        this.type = Objects.requireNonNull(type);
        this.contracts = List.copyOf(Objects.requireNonNull(contracts));
        if (this.contracts.isEmpty()) {
            throw new IllegalArgumentException("Composite contract must have at least one child");
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public ContractSeverity severity() {
        return severity;
    }

    @Override
    public ContractType type() {
        return type;
    }

    @Override
    public ContractVerdict check(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        List<ContractViolation> violations = new ArrayList<>();
        for (Contract contract : contracts) {
            ContractVerdict verdict = contract.check(testCase);
            if (!verdict.passed()) {
                violations.addAll(verdict.violations());
                if (contract.severity() == ContractSeverity.CRITICAL) {
                    break;
                }
            }
        }

        if (violations.isEmpty()) {
            return ContractVerdict.pass(name);
        }
        return new ContractVerdict(name, false, violations);
    }

    /**
     * Returns the child contracts in this suite.
     */
    public List<Contract> contracts() {
        return contracts;
    }
}
