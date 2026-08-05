package org.byteveda.agenteval.fingerprint;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapabilityProfilerTest {

    @Test
    void profilesAgentAcrossSingleDimension() {
        EvalMetric metric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.85, 0.7, "Good accuracy");
            }

            @Override
            public String name() {
                return "TestAccuracy";
            }
        };

        AgentTestCase testCase = AgentTestCase.builder()
                .input("What is 2+2?")
                .actualOutput("4")
                .expectedOutput("4")
                .build();

        CapabilityProfile profile = CapabilityProfiler.builder()
                .agentName("test-agent")
                .addBenchmark(new DimensionBenchmark(
                        CapabilityDimension.ACCURACY,
                        List.of(metric),
                        List.of(testCase)
                ))
                .build()
                .profile();

        assertEquals("test-agent", profile.agentName());
        assertEquals(1, profile.scores().size());
        assertTrue(profile.scores().containsKey(CapabilityDimension.ACCURACY));

        ProfileScore score = profile.scores().get(CapabilityDimension.ACCURACY);
        assertEquals(0.85, score.score(), 0.01);
        assertNotNull(score.reason());
        assertTrue(profile.durationMs() >= 0);
    }

    @Test
    void profilesMultipleDimensions() {
        EvalMetric highMetric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.9, 0.7, "High score");
            }

            @Override
            public String name() {
                return "HighMetric";
            }
        };

        EvalMetric lowMetric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.3, 0.7, "Low score");
            }

            @Override
            public String name() {
                return "LowMetric";
            }
        };

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .build();

        CapabilityProfile profile = CapabilityProfiler.builder()
                .agentName("multi-dim-agent")
                .addBenchmark(new DimensionBenchmark(
                        CapabilityDimension.ACCURACY,
                        List.of(highMetric),
                        List.of(testCase)
                ))
                .addBenchmark(new DimensionBenchmark(
                        CapabilityDimension.SAFETY,
                        List.of(lowMetric),
                        List.of(testCase)
                ))
                .build()
                .profile();

        assertEquals(2, profile.scores().size());
        assertEquals(0.6, profile.overallScore(), 0.01);
    }

    @Test
    void throwsWhenAgentNameMissing() {
        EvalMetric stubMetric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.pass("ok");
            }

            @Override
            public String name() {
                return "Stub";
            }
        };

        assertThrows(NullPointerException.class, () ->
                CapabilityProfiler.builder()
                        .addBenchmark(new DimensionBenchmark(
                                CapabilityDimension.ACCURACY,
                                List.of(stubMetric),
                                List.of(AgentTestCase.builder()
                                        .input("x")
                                        .actualOutput("y")
                                        .build())
                        ))
                        .build()
        );
    }

    @Test
    void throwsWhenNoBenchmarks() {
        assertThrows(IllegalArgumentException.class, () ->
                CapabilityProfiler.builder()
                        .agentName("empty-agent")
                        .build()
        );
    }
}
