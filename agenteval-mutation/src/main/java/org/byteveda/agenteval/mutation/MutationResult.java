package org.byteveda.agenteval.mutation;

import org.byteveda.agenteval.core.model.EvalScore;

import java.util.List;
import java.util.Objects;

/**
 * Result of applying a single mutation and evaluating the agent.
 *
 * @param mutatorName    the name of the mutator that was applied
 * @param originalPrompt the original system prompt before mutation
 * @param mutatedPrompt  the system prompt after mutation
 * @param scores         the evaluation scores for the mutated agent
 * @param detected       whether the mutation was detected (i.e., at least one score dropped below threshold)
 */
public record MutationResult(
        String mutatorName,
        String originalPrompt,
        String mutatedPrompt,
        List<EvalScore> scores,
        boolean detected
) {

    public MutationResult {
        Objects.requireNonNull(mutatorName, "mutatorName must not be null");
        Objects.requireNonNull(originalPrompt, "originalPrompt must not be null");
        Objects.requireNonNull(mutatedPrompt, "mutatedPrompt must not be null");
        scores = scores == null ? List.of() : List.copyOf(scores);
    }
}
