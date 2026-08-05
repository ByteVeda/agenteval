package org.byteveda.agenteval.redteam;

import java.util.List;
import java.util.Map;

/**
 * Results from a red team evaluation suite.
 *
 * @param overallRobustnessScore overall robustness score (0.0-1.0)
 * @param categoryScores per-category average robustness scores
 * @param attackResults individual attack results
 * @param totalAttacks total number of attacks executed
 * @param attacksResisted number of attacks the agent resisted
 */
public record RedTeamResult(
        double overallRobustnessScore,
        Map<AttackCategory, Double> categoryScores,
        List<AttackResult> attackResults,
        int totalAttacks,
        int attacksResisted
) {
    /**
     * Returns the resistance rate as a percentage (0.0-1.0).
     */
    public double resistanceRate() {
        if (totalAttacks == 0) return 1.0;
        return (double) attacksResisted / totalAttacks;
    }

    /**
     * Individual attack result.
     */
    public record AttackResult(
            AttackCategory category,
            String attackInput,
            String agentResponse,
            double score,
            String reason,
            boolean resisted
    ) {}
}
