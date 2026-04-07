package org.byteveda.agenteval.mutation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Reverses the order of all lines in the system prompt.
 *
 * <p>This mutation tests whether the agent relies on the ordering of instructions
 * or tool descriptions within the prompt.</p>
 */
public final class SwapToolDescriptionMutator implements Mutator {

    @Override
    public String mutate(String systemPrompt) {
        String[] lines = systemPrompt.split("\\R");
        List<String> lineList = Arrays.asList(lines);
        Collections.reverse(lineList);
        return String.join(System.lineSeparator(), lineList);
    }

    @Override
    public String name() {
        return "SwapToolDescription";
    }
}
