package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for built-in {@link InputGenerator} strategies.
 */
public final class InputGenerators {

    private InputGenerators() {}

    /**
     * LLM-powered generator that creates diverse adversarial inputs
     * specifically designed to test the given contracts.
     *
     * @param judge the LLM to use for generating inputs
     * @param inputsPerContract number of inputs to generate per contract
     */
    public static InputGenerator llmGenerated(JudgeModel judge, int inputsPerContract) {
        return new LLMInputGenerator(judge, inputsPerContract);
    }

    /**
     * Wraps raw input strings as test cases.
     */
    public static InputGenerator fromStrings(String... inputs) {
        List<AgentTestCase> cases = new ArrayList<>();
        for (String input : inputs) {
            cases.add(AgentTestCase.builder().input(input).build());
        }
        return contracts -> cases;
    }

    /**
     * Wraps pre-built test cases.
     */
    public static InputGenerator fromTestCases(List<AgentTestCase> testCases) {
        List<AgentTestCase> copy = List.copyOf(testCases);
        return contracts -> copy;
    }

    /**
     * Combines multiple generators into one.
     */
    public static InputGenerator combined(InputGenerator... generators) {
        return contracts -> {
            List<AgentTestCase> all = new ArrayList<>();
            for (InputGenerator g : generators) {
                all.addAll(g.generate(contracts));
            }
            return all;
        };
    }
}
