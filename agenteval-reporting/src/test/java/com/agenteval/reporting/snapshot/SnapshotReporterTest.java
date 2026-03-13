package com.agenteval.reporting.snapshot;

import com.agenteval.core.eval.CaseResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotReporterTest {

    @TempDir
    Path tempDir;

    @Test
    void firstRunCreatesBaseline() {
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(tempDir)
                .build();
        var reporter = new SnapshotReporter("baseline-test", config);

        reporter.report(makeResult(0.9));

        var store = new SnapshotStore(tempDir);
        assertThat(store.exists("baseline-test")).isTrue();
    }

    @Test
    void matchingResultsDoNotThrow() {
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(tempDir)
                .failOnRegression(true)
                .build();
        var reporter = new SnapshotReporter("stable", config);

        reporter.report(makeResult(0.9));
        // Same score should match
        reporter.report(makeResult(0.9));
    }

    @Test
    void regressionThrowsWhenConfigured() {
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(tempDir)
                .failOnRegression(true)
                .build();
        var reporter = new SnapshotReporter("regress", config);

        // Create baseline with high score
        reporter.report(makeResult(0.9));

        // Lower score should regress
        assertThatThrownBy(() -> reporter.report(makeResult(0.3)))
                .isInstanceOf(SnapshotRegressionException.class)
                .hasMessageContaining("regressions");
    }

    @Test
    void regressionAllowedWhenFailOnRegressionFalse() {
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(tempDir)
                .failOnRegression(false)
                .build();
        var reporter = new SnapshotReporter("lenient", config);

        reporter.report(makeResult(0.9));
        // Should not throw even with regression
        reporter.report(makeResult(0.3));
    }

    @Test
    void updateModeOverwritesExisting() {
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(tempDir)
                .updateSnapshots(true)
                .build();
        var reporter = new SnapshotReporter("update-test", config);
        var store = new SnapshotStore(tempDir);

        reporter.report(makeResult(0.9));
        reporter.report(makeResult(0.5));

        SnapshotData loaded = store.load("update-test").orElseThrow();
        assertThat(loaded.averageScore()).isCloseTo(0.5,
                org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    void compareOnlyReturnsEmptyWhenNoBaseline() {
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(tempDir)
                .build();
        var reporter = new SnapshotReporter("no-baseline", config);

        Optional<SnapshotComparisonResult> result = reporter.compareOnly(makeResult(0.9));
        assertThat(result).isEmpty();
    }

    @Test
    void compareOnlyReturnsResultWhenBaselineExists() {
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(tempDir)
                .build();
        var reporter = new SnapshotReporter("compare-test", config);

        // Create baseline
        reporter.report(makeResult(0.9));

        // Compare without saving
        Optional<SnapshotComparisonResult> result = reporter.compareOnly(makeResult(0.95));
        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(SnapshotStatus.IMPROVED);
    }

    @Test
    void improvementDetected() {
        SnapshotConfig config = SnapshotConfig.builder()
                .snapshotDirectory(tempDir)
                .failOnRegression(true)
                .build();
        var reporter = new SnapshotReporter("improve", config);

        reporter.report(makeResult(0.5));
        // Higher score should not throw
        reporter.report(makeResult(0.9));
    }

    private static EvalResult makeResult(double score) {
        AgentTestCase tc = AgentTestCase.builder()
                .input("What is Java?").actualOutput("A programming language").build();
        boolean passed = score >= 0.7;
        EvalScore evalScore = new EvalScore(score, 0.7, passed, "test", "TestMetric");
        CaseResult cr = new CaseResult(tc, Map.of("TestMetric", evalScore), passed);
        return EvalResult.of(List.of(cr), 100L);
    }
}
