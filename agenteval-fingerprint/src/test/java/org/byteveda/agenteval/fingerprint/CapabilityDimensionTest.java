package org.byteveda.agenteval.fingerprint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CapabilityDimensionTest {

    @Test
    void allDimensionsHaveDisplayName() {
        for (CapabilityDimension dim : CapabilityDimension.values()) {
            assertNotNull(dim.displayName());
        }
    }

    @Test
    void allDimensionsHaveDescription() {
        for (CapabilityDimension dim : CapabilityDimension.values()) {
            assertNotNull(dim.description());
        }
    }

    @Test
    void hasExpectedNumberOfDimensions() {
        assertEquals(8, CapabilityDimension.values().length);
    }

    @Test
    void accuracyDimensionHasCorrectDisplayName() {
        assertEquals("Accuracy", CapabilityDimension.ACCURACY.displayName());
    }

    @Test
    void safetyDimensionHasCorrectDisplayName() {
        assertEquals("Safety", CapabilityDimension.SAFETY.displayName());
    }

    @Test
    void toolUseDimensionHasCorrectDisplayName() {
        assertEquals("Tool Use", CapabilityDimension.TOOL_USE.displayName());
    }

    @Test
    void contextUtilizationHasCorrectDisplayName() {
        assertEquals("Context Utilization",
                CapabilityDimension.CONTEXT_UTILIZATION.displayName());
    }

    @Test
    void taskCompletionHasCorrectDisplayName() {
        assertEquals("Task Completion",
                CapabilityDimension.TASK_COMPLETION.displayName());
    }
}
