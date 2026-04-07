package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.model.AgentTestCase;

import java.util.List;

/**
 * Strategy for generating diverse inputs to stress-test contracts.
 */
@FunctionalInterface
public interface InputGenerator {

    /**
     * Generates test case inputs informed by the given contracts.
     *
     * @param contracts the contracts that will be verified
     * @return generated test cases (with input set, actualOutput blank)
     */
    List<AgentTestCase> generate(List<Contract> contracts);
}
