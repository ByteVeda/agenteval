package org.byteveda.agenteval.mutation;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Removes lines containing safety-related keywords from the system prompt.
 *
 * <p>Safety keywords include: safety, caution, warning, danger, harmful, toxic,
 * inappropriate, prohibited, forbidden, restrict.</p>
 */
public final class RemoveSafetyInstructionMutator implements Mutator {

    private static final Set<String> SAFETY_KEYWORDS = Set.of(
            "safety", "caution", "warning", "danger", "harmful",
            "toxic", "inappropriate", "prohibited", "forbidden", "restrict"
    );

    private static final Pattern LINE_SEPARATOR = Pattern.compile("\\R");

    @Override
    public String mutate(String systemPrompt) {
        String[] lines = LINE_SEPARATOR.split(systemPrompt);
        var result = new StringBuilder();
        boolean first = true;

        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            boolean containsSafetyKeyword = SAFETY_KEYWORDS.stream()
                    .anyMatch(lowerLine::contains);

            if (!containsSafetyKeyword) {
                if (!first) {
                    result.append(System.lineSeparator());
                }
                result.append(line);
                first = false;
            }
        }
        return result.toString();
    }

    @Override
    public String name() {
        return "RemoveSafetyInstruction";
    }
}
