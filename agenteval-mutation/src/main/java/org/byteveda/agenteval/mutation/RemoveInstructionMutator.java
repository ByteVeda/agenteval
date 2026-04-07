package org.byteveda.agenteval.mutation;

import java.util.regex.Pattern;

/**
 * Removes the first instruction line from the system prompt.
 *
 * <p>An instruction line is one that starts with a bullet marker ({@code - }, {@code * })
 * or a numbered prefix (e.g., {@code 1. }, {@code 2) }).</p>
 */
public final class RemoveInstructionMutator implements Mutator {

    private static final Pattern INSTRUCTION_LINE = Pattern.compile(
            "^(\\s*[-*]\\s|\\s*\\d+[.):]\\s).*$", Pattern.MULTILINE
    );

    @Override
    public String mutate(String systemPrompt) {
        var matcher = INSTRUCTION_LINE.matcher(systemPrompt);
        if (matcher.find()) {
            return systemPrompt.substring(0, matcher.start())
                    + systemPrompt.substring(matcher.end())
                            .replaceFirst("^\\R", "");
        }
        return systemPrompt;
    }

    @Override
    public String name() {
        return "RemoveInstruction";
    }
}
