package org.byteveda.agenteval.core.cost;

import org.byteveda.agenteval.core.model.TokenUsage;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe accumulator for tracking LLM usage costs.
 *
 * <p>Records token usage with a pricing model and tracks total cost
 * against an optional budget.</p>
 */
public final class CostTracker {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private final BigDecimal budget;
    private final AtomicReference<BigDecimal> totalCost = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicLong totalInputTokens = new AtomicLong();
    private final AtomicLong totalOutputTokens = new AtomicLong();

    public CostTracker() {
        this(null);
    }

    public CostTracker(BigDecimal budget) {
        this.budget = budget;
    }

    /**
     * Records token usage and calculates cost using the given pricing model.
     *
     * @throws BudgetExceededException if the budget is exceeded after recording
     */
    public void record(TokenUsage usage, PricingModel pricing) {
        if (usage == null) return;

        BigDecimal inputCost = pricing.inputCostPer1MTokens()
                .multiply(BigDecimal.valueOf(usage.inputTokens()))
                .divide(ONE_MILLION, MathContext.DECIMAL64);
        BigDecimal outputCost = pricing.outputCostPer1MTokens()
                .multiply(BigDecimal.valueOf(usage.outputTokens()))
                .divide(ONE_MILLION, MathContext.DECIMAL64);
        BigDecimal callCost = inputCost.add(outputCost);

        totalInputTokens.addAndGet(usage.inputTokens());
        totalOutputTokens.addAndGet(usage.outputTokens());
        BigDecimal newTotal = totalCost.accumulateAndGet(callCost, BigDecimal::add);

        if (isOverBudget(newTotal)) {
            throw new BudgetExceededException(newTotal, budget);
        }
    }

    public BigDecimal totalCost() {
        return totalCost.get();
    }

    public boolean isOverBudget() {
        return isOverBudget(totalCost.get());
    }

    private boolean isOverBudget(BigDecimal cost) {
        return budget != null && cost.compareTo(budget) > 0;
    }

    /**
     * Returns a snapshot of the current cost summary.
     */
    public CostSummary summary() {
        return new CostSummary(
                totalCost.get(),
                totalInputTokens.get(),
                totalOutputTokens.get());
    }
}
