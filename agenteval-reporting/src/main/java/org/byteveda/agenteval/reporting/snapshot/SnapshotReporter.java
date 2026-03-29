package org.byteveda.agenteval.reporting.snapshot;

import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.reporting.EvalReporter;
import org.byteveda.agenteval.reporting.regression.RegressionComparison;
import org.byteveda.agenteval.reporting.regression.RegressionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Reporter that saves evaluation results as named snapshots and compares
 * against prior baselines to detect regressions.
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>If {@code updateSnapshots} or env {@code AGENTEVAL_UPDATE_SNAPSHOTS=true}
 *       → save and return (overwrite existing)</li>
 *   <li>If no prior snapshot exists → save as baseline</li>
 *   <li>Otherwise → compare and fail on regressions if configured</li>
 * </ul>
 */
public final class SnapshotReporter implements EvalReporter {

    private static final Logger LOG = LoggerFactory.getLogger(SnapshotReporter.class);

    private final String snapshotName;
    private final SnapshotStore store;
    private final SnapshotConfig config;

    public SnapshotReporter(String snapshotName, SnapshotConfig config) {
        this.snapshotName = Objects.requireNonNull(snapshotName,
                "snapshotName must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.store = new SnapshotStore(config.snapshotDirectory());
    }

    @Override
    public void report(EvalResult result) {
        if (shouldUpdate()) {
            LOG.info("Updating snapshot '{}'", snapshotName);
            store.save(SnapshotData.from(snapshotName, result));
            return;
        }

        Optional<SnapshotData> existing = store.load(snapshotName);
        if (existing.isEmpty()) {
            LOG.info("Creating baseline snapshot '{}'", snapshotName);
            store.save(SnapshotData.from(snapshotName, result));
            return;
        }

        SnapshotComparisonResult comparison = doCompare(result, existing.get());
        if (comparison.status() == SnapshotStatus.REGRESSED
                && config.failOnRegression()) {
            throw new SnapshotRegressionException(
                    "Snapshot '" + snapshotName + "' has regressions: "
                            + comparison.regressionReport().newFailures() + " new failure(s), "
                            + "overall delta: "
                            + String.format("%.4f", comparison.regressionReport().overallDelta()),
                    comparison.regressionReport());
        }

        LOG.info("Snapshot '{}' comparison: {}", snapshotName, comparison.status());
    }

    /**
     * Compares an evaluation result against the stored snapshot without saving.
     *
     * @param result the current evaluation result
     * @return the comparison result, or empty if no baseline exists
     */
    public Optional<SnapshotComparisonResult> compareOnly(EvalResult result) {
        Optional<SnapshotData> existing = store.load(snapshotName);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(doCompare(result, existing.get()));
    }

    private SnapshotComparisonResult doCompare(EvalResult current, SnapshotData baseline) {
        EvalResult baselineResult = baseline.toEvalResult();
        RegressionReport report = RegressionComparison.compare(baselineResult, current);

        SnapshotStatus status;
        if (report.hasRegressions()) {
            status = SnapshotStatus.REGRESSED;
        } else if (report.overallDelta() > config.regressionThreshold()) {
            status = SnapshotStatus.IMPROVED;
        } else {
            status = SnapshotStatus.MATCHED;
        }

        return new SnapshotComparisonResult(snapshotName, status, report);
    }

    private boolean shouldUpdate() {
        if (config.updateSnapshots()) {
            return true;
        }
        String envUpdate = System.getenv("AGENTEVAL_UPDATE_SNAPSHOTS");
        return "true".equalsIgnoreCase(envUpdate);
    }
}
