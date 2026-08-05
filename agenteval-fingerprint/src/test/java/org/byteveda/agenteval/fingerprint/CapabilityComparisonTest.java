package org.byteveda.agenteval.fingerprint;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapabilityComparisonTest {

    @Test
    void detectsImprovements() {
        CapabilityProfile profileA = new CapabilityProfile(
                "agent-v1",
                Map.of(
                        CapabilityDimension.ACCURACY,
                        new ProfileScore(CapabilityDimension.ACCURACY, 0.7, "baseline"),
                        CapabilityDimension.SAFETY,
                        new ProfileScore(CapabilityDimension.SAFETY, 0.6, "baseline")
                ),
                1000
        );

        CapabilityProfile profileB = new CapabilityProfile(
                "agent-v2",
                Map.of(
                        CapabilityDimension.ACCURACY,
                        new ProfileScore(CapabilityDimension.ACCURACY, 0.9, "improved"),
                        CapabilityDimension.SAFETY,
                        new ProfileScore(CapabilityDimension.SAFETY, 0.8, "improved")
                ),
                1200
        );

        CapabilityComparisonResult result =
                CapabilityComparison.compare(profileA, profileB);

        assertEquals(2, result.improvements().size());
        assertTrue(result.regressions().isEmpty());
        assertTrue(result.overallDelta() > 0);
        assertEquals(0.2, result.deltas().get(CapabilityDimension.ACCURACY), 0.001);
        assertEquals(0.2, result.deltas().get(CapabilityDimension.SAFETY), 0.001);
    }

    @Test
    void detectsRegressions() {
        CapabilityProfile profileA = new CapabilityProfile(
                "agent-v1",
                Map.of(
                        CapabilityDimension.ACCURACY,
                        new ProfileScore(CapabilityDimension.ACCURACY, 0.9, "good")
                ),
                1000
        );

        CapabilityProfile profileB = new CapabilityProfile(
                "agent-v2",
                Map.of(
                        CapabilityDimension.ACCURACY,
                        new ProfileScore(CapabilityDimension.ACCURACY, 0.5, "regressed")
                ),
                1000
        );

        CapabilityComparisonResult result =
                CapabilityComparison.compare(profileA, profileB);

        assertTrue(result.improvements().isEmpty());
        assertEquals(1, result.regressions().size());
        assertTrue(result.overallDelta() < 0);
        assertEquals(-0.4, result.deltas().get(CapabilityDimension.ACCURACY), 0.001);
    }

    @Test
    void handlesIdenticalProfiles() {
        CapabilityProfile profile = new CapabilityProfile(
                "agent-v1",
                Map.of(
                        CapabilityDimension.ACCURACY,
                        new ProfileScore(CapabilityDimension.ACCURACY, 0.8, "same")
                ),
                1000
        );

        CapabilityComparisonResult result =
                CapabilityComparison.compare(profile, profile);

        assertTrue(result.improvements().isEmpty());
        assertTrue(result.regressions().isEmpty());
        assertEquals(0.0, result.overallDelta(), 0.001);
    }

    @Test
    void handlesMixedImprovementsAndRegressions() {
        CapabilityProfile profileA = new CapabilityProfile(
                "agent-v1",
                Map.of(
                        CapabilityDimension.ACCURACY,
                        new ProfileScore(CapabilityDimension.ACCURACY, 0.7, "A"),
                        CapabilityDimension.SAFETY,
                        new ProfileScore(CapabilityDimension.SAFETY, 0.9, "A")
                ),
                1000
        );

        CapabilityProfile profileB = new CapabilityProfile(
                "agent-v2",
                Map.of(
                        CapabilityDimension.ACCURACY,
                        new ProfileScore(CapabilityDimension.ACCURACY, 0.9, "B"),
                        CapabilityDimension.SAFETY,
                        new ProfileScore(CapabilityDimension.SAFETY, 0.6, "B")
                ),
                1000
        );

        CapabilityComparisonResult result =
                CapabilityComparison.compare(profileA, profileB);

        assertEquals(1, result.improvements().size());
        assertEquals(1, result.regressions().size());
        assertTrue(result.improvements().contains(CapabilityDimension.ACCURACY));
        assertTrue(result.regressions().contains(CapabilityDimension.SAFETY));
    }

    @Test
    void handlesEmptyProfiles() {
        CapabilityProfile profileA = new CapabilityProfile(
                "empty-a", Map.of(), 0);
        CapabilityProfile profileB = new CapabilityProfile(
                "empty-b", Map.of(), 0);

        CapabilityComparisonResult result =
                CapabilityComparison.compare(profileA, profileB);

        assertTrue(result.deltas().isEmpty());
        assertEquals(0.0, result.overallDelta(), 0.001);
    }
}
