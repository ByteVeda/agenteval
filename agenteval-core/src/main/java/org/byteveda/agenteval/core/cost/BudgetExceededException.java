package org.byteveda.agenteval.core.cost;

import java.math.BigDecimal;

/**
 * Thrown when the cost budget has been exceeded.
 */
public class BudgetExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final BigDecimal currentCost;
    private final BigDecimal budget;

    public BudgetExceededException(BigDecimal currentCost, BigDecimal budget) {
        super(String.format("Budget exceeded: $%s / $%s", currentCost, budget));
        this.currentCost = currentCost;
        this.budget = budget;
    }

    public BigDecimal getCurrentCost() { return currentCost; }
    public BigDecimal getBudget() { return budget; }
}
