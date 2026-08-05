package org.byteveda.agenteval.core.cost;

import org.byteveda.agenteval.core.model.TokenUsage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostTrackerTest {

    private static final PricingModel GPT4O_PRICING = new PricingModel(
            new BigDecimal("5.00"),
            new BigDecimal("15.00"),
            "openai");

    @Test
    void shouldTrackCost() {
        var tracker = new CostTracker();
        tracker.record(new TokenUsage(1000, 500, 1500), GPT4O_PRICING);

        assertThat(tracker.totalCost()).isGreaterThan(BigDecimal.ZERO);
        var summary = tracker.summary();
        assertThat(summary.totalInputTokens()).isEqualTo(1000);
        assertThat(summary.totalOutputTokens()).isEqualTo(500);
        assertThat(summary.totalTokens()).isEqualTo(1500);
    }

    @Test
    void shouldAccumulateCosts() {
        var tracker = new CostTracker();
        tracker.record(new TokenUsage(1000, 500, 1500), GPT4O_PRICING);
        tracker.record(new TokenUsage(2000, 1000, 3000), GPT4O_PRICING);

        var summary = tracker.summary();
        assertThat(summary.totalInputTokens()).isEqualTo(3000);
        assertThat(summary.totalOutputTokens()).isEqualTo(1500);
    }

    @Test
    void shouldThrowWhenBudgetExceeded() {
        var tracker = new CostTracker(new BigDecimal("0.001"));

        assertThatThrownBy(() ->
                tracker.record(new TokenUsage(1000000, 1000000, 2000000), GPT4O_PRICING))
                .isInstanceOf(BudgetExceededException.class);
    }

    @Test
    void shouldNotThrowWithinBudget() {
        var tracker = new CostTracker(new BigDecimal("100.00"));
        tracker.record(new TokenUsage(100, 50, 150), GPT4O_PRICING);

        assertThat(tracker.isOverBudget()).isFalse();
    }

    @Test
    void shouldHandleNullUsage() {
        var tracker = new CostTracker();
        tracker.record(null, GPT4O_PRICING);

        assertThat(tracker.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldStartAtZero() {
        var tracker = new CostTracker();
        assertThat(tracker.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tracker.isOverBudget()).isFalse();
    }
}
