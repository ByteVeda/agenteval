package com.agenteval.core.cost;

import java.math.BigDecimal;

/**
 * Summary of accumulated costs from LLM judge calls.
 *
 * @param totalCost the total cost in USD
 * @param totalInputTokens total input tokens consumed
 * @param totalOutputTokens total output tokens consumed
 */
public record CostSummary(
        BigDecimal totalCost,
        long totalInputTokens,
        long totalOutputTokens
) {
    public CostSummary {
        if (totalCost == null) totalCost = BigDecimal.ZERO;
    }

    public long totalTokens() {
        return totalInputTokens + totalOutputTokens;
    }
}
