package org.byteveda.agenteval.reporting.snapshot;

import org.byteveda.agenteval.reporting.ReportException;
import org.byteveda.agenteval.reporting.regression.RegressionReport;

import java.util.Objects;

/**
 * Thrown when snapshot comparison detects regressions.
 */
public class SnapshotRegressionException extends ReportException {

    private static final long serialVersionUID = 1L;

    private final transient RegressionReport regressionReport;

    public SnapshotRegressionException(String message, RegressionReport regressionReport) {
        super(message);
        this.regressionReport = Objects.requireNonNull(regressionReport,
                "regressionReport must not be null");
    }

    /**
     * Returns the regression report that triggered this exception.
     */
    public RegressionReport regressionReport() {
        return regressionReport;
    }
}
