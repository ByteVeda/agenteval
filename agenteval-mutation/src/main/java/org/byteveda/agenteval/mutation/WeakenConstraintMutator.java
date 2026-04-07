package org.byteveda.agenteval.mutation;

import java.util.regex.Pattern;

/**
 * Weakens constraint language in the system prompt.
 *
 * <p>Replaces strong directives with weaker alternatives:</p>
 * <ul>
 *   <li>{@code must} becomes {@code should}</li>
 *   <li>{@code always} becomes {@code usually}</li>
 *   <li>{@code never} becomes {@code try to avoid}</li>
 *   <li>{@code required} becomes {@code optional}</li>
 * </ul>
 */
public final class WeakenConstraintMutator implements Mutator {

    private static final Pattern MUST = Pattern.compile(
            "\\bmust\\b", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ALWAYS = Pattern.compile(
            "\\balways\\b", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NEVER = Pattern.compile(
            "\\bnever\\b", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REQUIRED = Pattern.compile(
            "\\brequired\\b", Pattern.CASE_INSENSITIVE
    );

    @Override
    public String mutate(String systemPrompt) {
        String result = systemPrompt;
        result = replacePreservingCase(MUST, result, "should");
        result = replacePreservingCase(ALWAYS, result, "usually");
        result = replacePreservingCase(NEVER, result, "try to avoid");
        result = replacePreservingCase(REQUIRED, result, "optional");
        return result;
    }

    private static String replacePreservingCase(Pattern pattern, String input,
            String replacement) {
        var matcher = pattern.matcher(input);
        var sb = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            String replaced;
            if (Character.isUpperCase(match.charAt(0))) {
                replaced = Character.toUpperCase(replacement.charAt(0))
                        + replacement.substring(1);
            } else {
                replaced = replacement;
            }
            matcher.appendReplacement(sb, replaced);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public String name() {
        return "WeakenConstraint";
    }
}
