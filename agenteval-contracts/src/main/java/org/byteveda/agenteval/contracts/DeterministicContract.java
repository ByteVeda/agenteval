package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.model.AgentTestCase;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A contract verified using deterministic predicate checks — no LLM calls needed.
 *
 * <p>Supports checks like substring matching, regex, tool call assertions, output length
 * bounds, and arbitrary predicates. All checks are combined with AND semantics.</p>
 *
 * @see Contracts
 * @see ContractBuilder
 */
public non-sealed class DeterministicContract implements Contract {

    private final String name;
    private final String description;
    private final ContractSeverity severity;
    private final ContractType type;
    private final Predicate<AgentTestCase> predicate;

    DeterministicContract(String name, String description,
            ContractSeverity severity, ContractType type,
            Predicate<AgentTestCase> predicate) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
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

        boolean holds = predicate.test(testCase);
        if (holds) {
            return ContractVerdict.pass(name);
        }
        return ContractVerdict.violation(name,
                "Contract violated: " + description, severity);
    }
}
