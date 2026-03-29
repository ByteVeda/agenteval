package org.byteveda.agenteval.reporting;

import org.byteveda.agenteval.core.eval.EvalResult;

/**
 * Interface for generating evaluation reports.
 */
public interface EvalReporter {

    /**
     * Generates a report for the given evaluation result.
     *
     * @param result the evaluation result to report
     * @throws ReportException if report generation fails
     */
    void report(EvalResult result);
}
