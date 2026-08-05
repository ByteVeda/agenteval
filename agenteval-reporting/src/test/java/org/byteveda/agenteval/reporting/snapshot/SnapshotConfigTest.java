package org.byteveda.agenteval.reporting.snapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class SnapshotConfigTest {

    @Test
    void defaultValues() {
        SnapshotConfig config = SnapshotConfig.defaults();

        assertThat(config.snapshotDirectory())
                .isEqualTo(Path.of("src/test/resources/agenteval-snapshots"));
        assertThat(config.updateSnapshots()).isFalse();
        assertThat(config.failOnRegression()).isTrue();
        assertThat(config.regressionThreshold()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void customValues() {
        Path customDir = Path.of("/tmp/custom-snaps");
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(customDir)
                .updateSnapshots(true)
                .failOnRegression(false)
                .regressionThreshold(0.05)
                .build();

        assertThat(config.snapshotDirectory()).isEqualTo(customDir);
        assertThat(config.updateSnapshots()).isTrue();
        assertThat(config.failOnRegression()).isFalse();
        assertThat(config.regressionThreshold()).isCloseTo(0.05, within(0.001));
    }

    @Test
    void rejectsNullDirectory() {
        assertThatThrownBy(() -> SnapshotConfig.builder().snapshotDirectory(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidRegressionThreshold() {
        assertThatThrownBy(() -> SnapshotConfig.builder().regressionThreshold(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SnapshotConfig.builder().regressionThreshold(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validBoundaryThresholds() {
        SnapshotConfig lower = SnapshotConfig.builder().regressionThreshold(0.0).build();
        assertThat(lower.regressionThreshold()).isCloseTo(0.0, within(0.001));

        SnapshotConfig upper = SnapshotConfig.builder().regressionThreshold(1.0).build();
        assertThat(upper.regressionThreshold()).isCloseTo(1.0, within(0.001));
    }
}
