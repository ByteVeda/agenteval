package org.byteveda.agenteval.mutation;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A user-supplied mutator backed by a {@link UnaryOperator}.
 *
 * @param mutatorName a descriptive name for this mutator
 * @param operator    the mutation function
 */
public record PluggableMutator(
        String mutatorName,
        UnaryOperator<String> operator
) implements Mutator {

    public PluggableMutator {
        Objects.requireNonNull(mutatorName, "mutatorName must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
    }

    @Override
    public String mutate(String systemPrompt) {
        return operator.apply(systemPrompt);
    }

    @Override
    public String name() {
        return mutatorName;
    }
}
