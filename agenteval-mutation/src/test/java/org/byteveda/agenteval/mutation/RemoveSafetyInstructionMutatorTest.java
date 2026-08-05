package org.byteveda.agenteval.mutation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoveSafetyInstructionMutatorTest {

    private final RemoveSafetyInstructionMutator mutator = new RemoveSafetyInstructionMutator();

    @Test
    void removesSafetyLine() {
        String input = "Be helpful.\nEnsure safety at all times.\nRespond in JSON.";
        String result = mutator.mutate(input);
        assertFalse(result.contains("safety"));
        assertTrue(result.contains("Be helpful."));
        assertTrue(result.contains("Respond in JSON."));
    }

    @Test
    void removesMultipleSafetyLines() {
        String input = String.join(System.lineSeparator(),
                "Line one.",
                "Warning: do not share secrets.",
                "Line three.",
                "This content is toxic and prohibited.",
                "Line five."
        );
        String result = mutator.mutate(input);
        assertFalse(result.toLowerCase().contains("warning"));
        assertFalse(result.toLowerCase().contains("toxic"));
        assertFalse(result.toLowerCase().contains("prohibited"));
        assertTrue(result.contains("Line one."));
        assertTrue(result.contains("Line three."));
        assertTrue(result.contains("Line five."));
    }

    @Test
    void leavesPromptUnchangedWhenNoSafetyKeywords() {
        String input = "You are a helpful assistant.\nRespond clearly.";
        String result = mutator.mutate(input);
        assertTrue(result.contains("You are a helpful assistant."));
        assertTrue(result.contains("Respond clearly."));
    }

    @Test
    void handlesCaseInsensitiveKeywords() {
        String input = "Follow SAFETY protocols.\nBe kind.";
        String result = mutator.mutate(input);
        assertFalse(result.toLowerCase().contains("safety"));
        assertTrue(result.contains("Be kind."));
    }

    @Test
    void handlesEmptyPrompt() {
        String result = mutator.mutate("");
        assertEquals("", result);
    }

    @Test
    void returnsNameCorrectly() {
        assertEquals("RemoveSafetyInstruction", mutator.name());
    }
}
