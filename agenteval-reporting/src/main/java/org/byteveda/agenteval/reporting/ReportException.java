package org.byteveda.agenteval.reporting;

/**
 * Unchecked exception for report generation errors.
 */
public class ReportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReportException(String message) {
        super(message);
    }

    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
