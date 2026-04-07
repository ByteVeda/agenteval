package org.byteveda.agenteval.metrics.cost;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CostNormalizedMetricTest {

    private static final double DELTA = 0.001;

    private EvalMetric stubMetric(double score) {
        EvalMetric base = mock(EvalMetric.class);
        when(base.name()).thenReturn("TestMetric");
        when(base.evaluate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(EvalScore.of(score, 0.5, "base reason"));
        return base;
    }

    @Test
    void normalizeWithLowerCostBoostsScore() {
        EvalMetric base = stubMetric(0.6);
        CostNormalizedMetric metric = new CostNormalizedMetric(
                base, new BigDecimal("0.10"), 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .cost(new BigDecimal("0.05"))
                .build();

        EvalScore score = metric.evaluate(testCase);
        // costRatio = 0.10 / 0.05 = 2.0, normalized = min(1.0, 0.6 * 2.0) = 1.0
        assertEquals(1.0, score.value(), DELTA);
        assertTrue(score.passed());
    }

    @Test
    void normalizeWithHigherCostReducesScore() {
        EvalMetric base = stubMetric(0.8);
        CostNormalizedMetric metric = new CostNormalizedMetric(
                base, new BigDecimal("0.05"), 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .cost(new BigDecimal("0.10"))
                .build();

        EvalScore score = metric.evaluate(testCase);
        // costRatio = 0.05 / 0.10 = 0.5, normalized = 0.8 * 0.5 = 0.4
        assertEquals(0.4, score.value(), DELTA);
        assertFalse(score.passed());
    }

    @Test
    void normalizeWithEqualCostPreservesScore() {
        EvalMetric base = stubMetric(0.75);
        CostNormalizedMetric metric = new CostNormalizedMetric(
                base, new BigDecimal("0.10"), 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .cost(new BigDecimal("0.10"))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertEquals(0.75, score.value(), DELTA);
    }

    @Test
    void noCostDataFallsBackToBaseScore() {
        EvalMetric base = stubMetric(0.9);
        CostNormalizedMetric metric = new CostNormalizedMetric(
                base, new BigDecimal("0.10"), 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertEquals(0.9, score.value(), DELTA);
        assertTrue(score.reason().contains("Cost data unavailable"));
    }

    @Test
    void zeroCostFallsBackToBaseScore() {
        EvalMetric base = stubMetric(0.7);
        CostNormalizedMetric metric = new CostNormalizedMetric(
                base, new BigDecimal("0.10"), 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .cost(BigDecimal.ZERO)
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertEquals(0.7, score.value(), DELTA);
    }

    @Test
    void nameIncludesCostNormalizedSuffix() {
        EvalMetric base = stubMetric(0.5);
        CostNormalizedMetric metric = new CostNormalizedMetric(
                base, new BigDecimal("0.10"), 0.5);

        assertEquals("TestMetric/CostNormalized", metric.name());
    }

    @Test
    void rejectsNullBaseMetric() {
        assertThrows(NullPointerException.class,
                () -> new CostNormalizedMetric(null, new BigDecimal("0.10"), 0.5));
    }

    @Test
    void rejectsNullReferenceCost() {
        EvalMetric base = stubMetric(0.5);
        assertThrows(NullPointerException.class,
                () -> new CostNormalizedMetric(base, null, 0.5));
    }

    @Test
    void rejectsZeroReferenceCost() {
        EvalMetric base = stubMetric(0.5);
        assertThrows(IllegalArgumentException.class,
                () -> new CostNormalizedMetric(base, BigDecimal.ZERO, 0.5));
    }

    @Test
    void rejectsNegativeReferenceCost() {
        EvalMetric base = stubMetric(0.5);
        assertThrows(IllegalArgumentException.class,
                () -> new CostNormalizedMetric(base, new BigDecimal("-0.01"), 0.5));
    }

    @Test
    void normalizedScoreClampedToZero() {
        // base score is 0.0 so normalized should be 0.0 regardless of cost ratio
        EvalMetric base = stubMetric(0.0);
        CostNormalizedMetric metric = new CostNormalizedMetric(
                base, new BigDecimal("0.10"), 0.5);

        AgentTestCase testCase = AgentTestCase.builder()
                .input("test input")
                .actualOutput("test output")
                .cost(new BigDecimal("0.01"))
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertEquals(0.0, score.value(), DELTA);
    }
}
