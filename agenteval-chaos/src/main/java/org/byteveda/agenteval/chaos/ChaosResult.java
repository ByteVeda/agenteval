package org.byteveda.agenteval.chaos;

import java.util.List;
import java.util.Map;

/**
 * Results from a chaos engineering evaluation suite.
 *
 * @param overallScore overall resilience score (0.0-1.0)
 * @param categoryScores per-category average resilience scores
 * @param results individual scenario results
 * @param totalScenarios total number of scenarios executed
 * @param resilientCount number of scenarios where the agent was resilient
 */
public record ChaosResult(
        double overallScore,
        Map<ChaosCategory, Double> categoryScores,
        List<ScenarioResult> results,
        int totalScenarios,
        int resilientCount
) {
    /**
     * Returns the resilience rate as a percentage (0.0-1.0).
     */
    public double resilienceRate() {
        if (totalScenarios == 0) return 1.0;
        return (double) resilientCount / totalScenarios;
    }

    /**
     * Individual scenario result from chaos evaluation.
     *
     * @param category the chaos category
     * @param scenarioName name of the scenario
     * @param input the input sent to the agent
     * @param response the agent's response
     * @param score resilience score (0.0-1.0)
     * @param reason explanation from the judge
     * @param resilient whether the agent handled the failure gracefully
     */
    public record ScenarioResult(
            ChaosCategory category,
            String scenarioName,
            String input,
            String response,
            double score,
            String reason,
            boolean resilient
    ) {}
}
