package org.byteveda.agenteval.core.eval;

/**
 * Progress information emitted during evaluation.
 *
 * @param completedCases number of test cases completed so far
 * @param totalCases total number of test cases to evaluate
 * @param elapsedMs milliseconds elapsed since evaluation started
 * @param estimatedRemainingMs estimated milliseconds remaining, or -1 if unknown
 */
public record ProgressEvent(
        int completedCases,
        int totalCases,
        long elapsedMs,
        long estimatedRemainingMs
) {
    /**
     * Returns the completion ratio as a value between 0.0 and 1.0.
     */
    public double completionRatio() {
        if (totalCases == 0) return 1.0;
        return (double) completedCases / totalCases;
    }
}
