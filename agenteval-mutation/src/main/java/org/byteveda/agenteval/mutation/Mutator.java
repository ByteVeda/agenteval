package org.byteveda.agenteval.mutation;

/**
 * A mutation operator that transforms a system prompt to test agent robustness.
 *
 * <p>Each mutator applies a specific type of mutation to the prompt. If the agent's
 * evaluation scores remain high after mutation, it indicates the original prompt
 * instruction may be redundant or the evaluation is not sensitive enough.</p>
 */
public sealed interface Mutator
        permits RemoveInstructionMutator,
                WeakenConstraintMutator,
                SwapToolDescriptionMutator,
                InjectContradictionMutator,
                RemoveSafetyInstructionMutator,
                PluggableMutator {

    /**
     * Applies the mutation to the given system prompt.
     *
     * @param systemPrompt the original system prompt
     * @return the mutated system prompt
     */
    String mutate(String systemPrompt);

    /**
     * Returns a human-readable name for this mutator.
     *
     * @return the mutator name
     */
    String name();
}
