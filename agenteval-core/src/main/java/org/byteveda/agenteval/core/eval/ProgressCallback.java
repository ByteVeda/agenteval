package org.byteveda.agenteval.core.eval;

/**
 * Callback invoked during evaluation to report progress.
 */
@FunctionalInterface
public interface ProgressCallback {

    /**
     * Called after each test case completes evaluation.
     *
     * @param event the progress event containing current status and ETA
     */
    void onProgress(ProgressEvent event);
}
