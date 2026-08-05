package org.byteveda.agenteval.metrics.cost;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CostEfficiencyAnalyzerTest {

    private static final double DELTA = 0.001;

    private CaseResult makeCaseResult(String input, double score, boolean passed,
            BigDecimal cost) {
        AgentTestCase testCase = AgentTestCase.builder()
                .input(input)
                .actualOutput("output for " + input)
                .cost(cost)
                .build();
        Map<String, EvalScore> scores = Map.of(
                "TestMetric", new EvalScore(score, 0.5, passed, "reason", "TestMetric"));
        return new CaseResult(testCase, scores, passed);
    }

    @Test
    void analyzeComputesTotalCost() {
        CaseResult cr1 = makeCaseResult("q1", 0.8, true, new BigDecimal("0.05"));
        CaseResult cr2 = makeCaseResult("q2", 0.6, true, new BigDecimal("0.03"));
        CaseResult cr3 = makeCaseResult("q3", 0.3, false, new BigDecimal("0.02"));
        EvalResult result = EvalResult.of(List.of(cr1, cr2, cr3), 1000L);

        CostEfficiencyReport report = CostEfficiencyAnalyzer.analyze(result);

        assertEquals(new BigDecimal("0.10"), report.totalCost());
        assertEquals(3, report.totalCost().divide(report.costPerCase(),
                0, java.math.RoundingMode.HALF_UP).intValue());
    }

    @Test
    void analyzeComputesPassRate() {
        CaseResult cr1 = makeCaseResult("q1", 0.8, true, new BigDecimal("0.05"));
        CaseResult cr2 = makeCaseResult("q2", 0.6, true, new BigDecimal("0.03"));
        CaseResult cr3 = makeCaseResult("q3", 0.3, false, new BigDecimal("0.02"));
        EvalResult result = EvalResult.of(List.of(cr1, cr2, cr3), 1000L);

        CostEfficiencyReport report = CostEfficiencyAnalyzer.analyze(result);

        assertEquals(2.0 / 3.0, report.passRate(), DELTA);
    }

    @Test
    void analyzeComputesCostPerPassingCase() {
        CaseResult cr1 = makeCaseResult("q1", 0.8, true, new BigDecimal("0.06"));
        CaseResult cr2 = makeCaseResult("q2", 0.3, false, new BigDecimal("0.04"));
        EvalResult result = EvalResult.of(List.of(cr1, cr2), 500L);

        CostEfficiencyReport report = CostEfficiencyAnalyzer.analyze(result);

        // total cost = 0.10, 1 passing case, cost/passing = 0.10
        assertEquals(0, new BigDecimal("0.10").compareTo(
                report.costPerPassingCase().setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void analyzeHandlesZeroCases() {
        EvalResult result = EvalResult.of(List.of(), 0L);

        CostEfficiencyReport report = CostEfficiencyAnalyzer.analyze(result);

        assertEquals(BigDecimal.ZERO, report.totalCost());
        assertEquals(0.0, report.passRate(), DELTA);
        assertEquals(0.0, report.costEfficiencyRatio(), DELTA);
    }

    @Test
    void analyzeHandlesNullCostOnCases() {
        CaseResult cr1 = makeCaseResult("q1", 0.8, true, null);
        CaseResult cr2 = makeCaseResult("q2", 0.6, true, new BigDecimal("0.05"));
        EvalResult result = EvalResult.of(List.of(cr1, cr2), 500L);

        CostEfficiencyReport report = CostEfficiencyAnalyzer.analyze(result);

        assertEquals(0, new BigDecimal("0.05").compareTo(report.totalCost()));
    }

    @Test
    void analyzeRejectsNullResult() {
        assertThrows(NullPointerException.class,
                () -> CostEfficiencyAnalyzer.analyze(null));
    }

    @Test
    void paretoFrontierIdentifiesOptimalVariants() {
        // Variant A: high score, high cost
        CaseResult crA = makeCaseResult("q1", 0.9, true, new BigDecimal("1.00"));
        EvalResult resultA = EvalResult.of(List.of(crA), 1000L);

        // Variant B: medium score, low cost (Pareto-optimal)
        CaseResult crB = makeCaseResult("q1", 0.7, true, new BigDecimal("0.10"));
        EvalResult resultB = EvalResult.of(List.of(crB), 500L);

        // Variant C: low score, high cost (dominated by both A and B)
        CaseResult crC = makeCaseResult("q1", 0.5, true, new BigDecimal("1.00"));
        EvalResult resultC = EvalResult.of(List.of(crC), 2000L);

        Map<String, EvalResult> variants = new LinkedHashMap<>();
        variants.put("VariantA", resultA);
        variants.put("VariantB", resultB);
        variants.put("VariantC", resultC);

        ParetoFrontier frontier = CostEfficiencyAnalyzer.paretoFrontier(variants);

        assertNotNull(frontier);
        assertEquals(3, frontier.points().size());

        // A and B should be Pareto-optimal, C should be dominated
        for (ParetoPoint p : frontier.points()) {
            if ("VariantA".equals(p.variantName())) {
                assertTrue(p.paretoOptimal(), "VariantA should be Pareto-optimal");
            } else if ("VariantB".equals(p.variantName())) {
                assertTrue(p.paretoOptimal(), "VariantB should be Pareto-optimal");
            } else if ("VariantC".equals(p.variantName())) {
                assertFalse(p.paretoOptimal(), "VariantC should be dominated");
            }
        }

        assertTrue(frontier.dominatedVariants().contains("VariantC"));
        assertFalse(frontier.dominatedVariants().contains("VariantA"));
        assertFalse(frontier.dominatedVariants().contains("VariantB"));
    }

    @Test
    void paretoFrontierSingleVariantIsOptimal() {
        CaseResult cr = makeCaseResult("q1", 0.8, true, new BigDecimal("0.50"));
        EvalResult result = EvalResult.of(List.of(cr), 1000L);

        Map<String, EvalResult> variants = Map.of("Only", result);
        ParetoFrontier frontier = CostEfficiencyAnalyzer.paretoFrontier(variants);

        assertEquals(1, frontier.points().size());
        assertTrue(frontier.points().get(0).paretoOptimal());
        assertTrue(frontier.dominatedVariants().isEmpty());
    }

    @Test
    void paretoFrontierRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> CostEfficiencyAnalyzer.paretoFrontier(null));
    }
}
