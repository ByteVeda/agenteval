package org.byteveda.agenteval.metrics.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class VectorMathTest {

    @Test
    void identicalVectorsShouldReturnOne() {
        var v = List.of(1.0, 2.0, 3.0);
        assertThat(VectorMath.cosineSimilarity(v, v)).isCloseTo(1.0, within(0.001));
    }

    @Test
    void orthogonalVectorsShouldReturnZero() {
        var a = List.of(1.0, 0.0);
        var b = List.of(0.0, 1.0);
        assertThat(VectorMath.cosineSimilarity(a, b)).isCloseTo(0.0, within(0.001));
    }

    @Test
    void oppositeVectorsShouldReturnNegativeOne() {
        var a = List.of(1.0, 0.0);
        var b = List.of(-1.0, 0.0);
        assertThat(VectorMath.cosineSimilarity(a, b)).isCloseTo(-1.0, within(0.001));
    }

    @Test
    void zeroVectorShouldReturnZero() {
        var a = List.of(0.0, 0.0, 0.0);
        var b = List.of(1.0, 2.0, 3.0);
        assertThat(VectorMath.cosineSimilarity(a, b)).isEqualTo(0.0);
    }

    @Test
    void shouldRejectDifferentDimensions() {
        assertThatThrownBy(() -> VectorMath.cosineSimilarity(
                List.of(1.0, 2.0), List.of(1.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions must match");
    }
}
