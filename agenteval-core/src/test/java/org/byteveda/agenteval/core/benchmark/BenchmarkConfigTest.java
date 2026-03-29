package org.byteveda.agenteval.core.benchmark;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BenchmarkConfigTest {

    @Test
    void defaultValues() {
        BenchmarkConfig config = BenchmarkConfig.defaults();
        assertThat(config.parallelVariants()).isFalse();
        assertThat(config.maxParallelVariants()).isPositive();
    }

    @Test
    void customValues() {
        BenchmarkConfig config = BenchmarkConfig.builder()
                .parallelVariants(true)
                .maxParallelVariants(8)
                .build();

        assertThat(config.parallelVariants()).isTrue();
        assertThat(config.maxParallelVariants()).isEqualTo(8);
    }

    @Test
    void rejectsZeroParallelVariants() {
        assertThatThrownBy(() -> BenchmarkConfig.builder().maxParallelVariants(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeParallelVariants() {
        assertThatThrownBy(() -> BenchmarkConfig.builder().maxParallelVariants(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
