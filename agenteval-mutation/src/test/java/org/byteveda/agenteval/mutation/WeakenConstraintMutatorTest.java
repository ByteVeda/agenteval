package org.byteveda.agenteval.mutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WeakenConstraintMutatorTest {

    private final WeakenConstraintMutator mutator = new WeakenConstraintMutator();

    @Test
    void replacesMustwithShould() {
        String input = "You must respond in JSON format.";
        String result = mutator.mutate(input);
        assertEquals("You should respond in JSON format.", result);
    }

    @Test
    void replacesAlwaysWithUsually() {
        String input = "Always include a summary.";
        String result = mutator.mutate(input);
        assertEquals("Usually include a summary.", result);
    }

    @Test
    void replacesNeverWithTryToAvoid() {
        String input = "Never disclose personal data.";
        String result = mutator.mutate(input);
        assertEquals("Try to avoid disclose personal data.", result);
    }

    @Test
    void replacesRequiredWithOptional() {
        String input = "Authentication is required for all endpoints.";
        String result = mutator.mutate(input);
        assertEquals("Authentication is optional for all endpoints.", result);
    }

    @Test
    void handlesMultipleReplacementsInSameText() {
        String input = "You must always respond and never fail. This is required.";
        String result = mutator.mutate(input);
        assertEquals(
                "You should usually respond and try to avoid fail. This is optional.",
                result
        );
    }

    @Test
    void preservesCaseOfFirstCharacter() {
        String input = "MUST follow the rules. Never break them.";
        String result = mutator.mutate(input);
        assertFalse(result.contains("MUST"));
        assertFalse(result.contains("Never"));
    }

    @Test
    void leavesUnrelatedTextUnchanged() {
        String input = "This prompt has no constraint keywords.";
        String result = mutator.mutate(input);
        assertEquals(input, result);
    }

    @Test
    void returnsNameCorrectly() {
        assertEquals("WeakenConstraint", mutator.name());
    }
}
