package org.byteveda.agenteval.metrics.cost;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LatencyNormalizedMetricTest {

    private static final double DELTA = 0.001;

    private EvalMetric stubMetric(double score) {
        EvalMetric base = mock(EvalMetric.class);
        when(base.name()).thenReturn("TestMetric");
        when(base.evaluate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(EvalScore.of(score, 0.5, "base reason"));
        return base;
    }

    @Test
    void normalizeWithLowerLatencyBoostsScore() {
        EvalMetric base = stubMetric(0.6);
        LatencyNormalizedMetric metric = new LatencyNormalizedMetric(base, 1000L, 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .latencyMs(500L)
                .build();

        EvalScore score = metric.evaluate(testCase);
        // latencyRatio = 1000 / 500 = 2.0, normalized = min(1.0, 0.6 * 2.0) = 1.0
        assertEquals(1.0, score.value(), DELTA);
        assertTrue(score.passed());
    }

    @Test
    void normalizeWithHigherLatencyReducesScore() {
        EvalMetric base = stubMetric(0.8);
        LatencyNormalizedMetric metric = new LatencyNormalizedMetric(base, 500L, 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .latencyMs(2000L)
                .build();

        EvalScore score = metric.evaluate(testCase);
        // latencyRatio = 500 / 2000 = 0.25, normalized = 0.8 * 0.25 = 0.2
        assertEquals(0.2, score.value(), DELTA);
        assertFalse(score.passed());
    }

    @Test
    void normalizeWithEqualLatencyPreservesScore() {
        EvalMetric base = stubMetric(0.75);
        LatencyNormalizedMetric metric = new LatencyNormalizedMetric(base, 1000L, 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .latencyMs(1000L)
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertEquals(0.75, score.value(), DELTA);
    }

    @Test
    void noLatencyDataFallsBackToBaseScore() {
        EvalMetric base = stubMetric(0.9);
        LatencyNormalizedMetric metric = new LatencyNormalizedMetric(base, 1000L, 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .build();

        EvalScore score = metric.evaluate(testCase);
        // latencyMs defaults to 0, so falls back to base score
        assertEquals(0.9, score.value(), DELTA);
        assertTrue(score.reason().contains("Latency data unavailable"));
    }

    @Test
    void nameIncludesLatencyNormalizedSuffix() {
        EvalMetric base = stubMetric(0.5);
        LatencyNormalizedMetric metric = new LatencyNormalizedMetric(base, 1000L, 0.5);

        assertEquals("TestMetric/LatencyNormalized", metric.name());
    }

    @Test
    void rejectsNullBaseMetric() {
        assertThrows(NullPointerException.class,
                () -> new LatencyNormalizedMetric(null, 1000L, 0.5));
    }

    @Test
    void rejectsZeroReferenceLatency() {
        EvalMetric base = stubMetric(0.5);
        assertThrows(IllegalArgumentException.class,
                () -> new LatencyNormalizedMetric(base, 0L, 0.5));
    }

    @Test
    void rejectsNegativeReferenceLatency() {
        EvalMetric base = stubMetric(0.5);
        assertThrows(IllegalArgumentException.class,
                () -> new LatencyNormalizedMetric(base, -100L, 0.5));
    }

    @Test
    void normalizedScoreClampedToZero() {
        EvalMetric base = stubMetric(0.0);
        LatencyNormalizedMetric metric = new LatencyNormalizedMetric(base, 1000L, 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .latencyMs(100L)
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertEquals(0.0, score.value(), DELTA);
    }
}
