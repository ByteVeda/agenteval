package org.byteveda.agenteval.core.cost;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingModelTest {

    @Test
    void shouldCreateValidPricingModel() {
        var pricing = new PricingModel(
                new BigDecimal("5.00"),
                new BigDecimal("15.00"),
                "openai");

        assertThat(pricing.inputCostPer1MTokens()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(pricing.outputCostPer1MTokens()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(pricing.provider()).isEqualTo("openai");
    }

    @Test
    void shouldRejectNegativeInputCost() {
        assertThatThrownBy(() -> new PricingModel(
                new BigDecimal("-1"), BigDecimal.ONE, "test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativeOutputCost() {
        assertThatThrownBy(() -> new PricingModel(
                BigDecimal.ONE, new BigDecimal("-1"), "test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankProvider() {
        assertThatThrownBy(() -> new PricingModel(
                BigDecimal.ONE, BigDecimal.ONE, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
