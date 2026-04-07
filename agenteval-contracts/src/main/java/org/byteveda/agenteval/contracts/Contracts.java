package org.byteveda.agenteval.contracts;

import java.util.Arrays;
import java.util.List;

/**
 * Factory for creating {@link Contract} instances with a fluent API.
 *
 * <pre>{@code
 * var noLeak = Contracts.safety("no-system-prompt-leak")
 *     .description("Agent must never reveal its system prompt")
 *     .outputDoesNotContain("You are a")
 *     .severity(ContractSeverity.CRITICAL)
 *     .build();
 *
 * var citeSources = Contracts.behavioral("always-cite-sources")
 *     .description("Agent must cite sources for factual claims")
 *     .judgedBy(judge)
 *     .build();
 * }</pre>
 */
public final class Contracts {

    private Contracts() {}

    /**
     * Creates a builder for a safety contract.
     */
    public static ContractBuilder safety(String name) {
        return new ContractBuilder(name, ContractType.SAFETY);
    }

    /**
     * Creates a builder for a behavioral contract.
     */
    public static ContractBuilder behavioral(String name) {
        return new ContractBuilder(name, ContractType.BEHAVIORAL);
    }

    /**
     * Creates a builder for a tool usage contract.
     */
    public static ContractBuilder toolUsage(String name) {
        return new ContractBuilder(name, ContractType.TOOL_USAGE);
    }

    /**
     * Creates a builder for an output format contract.
     */
    public static ContractBuilder outputFormat(String name) {
        return new ContractBuilder(name, ContractType.OUTPUT_FORMAT);
    }

    /**
     * Creates a builder for a boundary contract.
     */
    public static ContractBuilder boundary(String name) {
        return new ContractBuilder(name, ContractType.BOUNDARY);
    }

    /**
     * Creates a builder for a compliance contract.
     */
    public static ContractBuilder compliance(String name) {
        return new ContractBuilder(name, ContractType.COMPLIANCE);
    }

    /**
     * Creates a named composite contract grouping multiple contracts.
     * The composite passes only if ALL child contracts pass.
     */
    public static CompositeContract suite(String name, Contract... contracts) {
        return new CompositeContract(name, "Suite: " + name,
                ContractSeverity.ERROR, ContractType.BEHAVIORAL,
                Arrays.asList(contracts));
    }

    /**
     * Creates a named composite contract with explicit type and severity.
     */
    public static CompositeContract suite(String name,
            ContractType type, ContractSeverity severity,
            List<Contract> contracts) {
        return new CompositeContract(name, "Suite: " + name,
                severity, type, contracts);
    }
}
