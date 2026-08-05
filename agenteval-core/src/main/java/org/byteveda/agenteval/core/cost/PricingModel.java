package org.byteveda.agenteval.core.cost;

import java.math.BigDecimal;

/**
 * Pricing configuration for a specific LLM model.
 *
 * @param inputCostPer1MTokens cost per 1 million input tokens
 * @param outputCostPer1MTokens cost per 1 million output tokens
 * @param provider the provider name (e.g., "openai", "anthropic")
 */
public record PricingModel(
        BigDecimal inputCostPer1MTokens,
        BigDecimal outputCostPer1MTokens,
        String provider
) {
    public PricingModel {
        if (inputCostPer1MTokens == null || inputCostPer1MTokens.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("inputCostPer1MTokens must be non-negative");
        }
        if (outputCostPer1MTokens == null || outputCostPer1MTokens.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("outputCostPer1MTokens must be non-negative");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
    }
}
