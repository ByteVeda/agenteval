package org.byteveda.agenteval.core.eval;

import java.io.PrintStream;

/**
 * Default {@link ProgressCallback} implementation that prints a progress bar to stderr.
 */
public final class ConsoleProgressBar implements ProgressCallback {

    private static final int BAR_WIDTH = 30;

    private final PrintStream out;

    public ConsoleProgressBar() {
        this(System.err);
    }

    ConsoleProgressBar(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onProgress(ProgressEvent event) {
        int completed = event.completedCases();
        int total = event.totalCases();
        double ratio = event.completionRatio();

        int filled = (int) (ratio * BAR_WIDTH);
        var bar = new StringBuilder("[");
        for (int i = 0; i < BAR_WIDTH; i++) {
            bar.append(i < filled ? '#' : '.');
        }
        bar.append(']');

        String eta;
        if (event.estimatedRemainingMs() < 0) {
            eta = "ETA: --";
        } else {
            long secs = event.estimatedRemainingMs() / 1000;
            eta = String.format("ETA: %ds", secs);
        }

        out.printf("\r%s %d/%d (%.0f%%) %s", bar, completed, total, ratio * 100, eta);

        if (completed == total) {
            out.println();
        }
    }
}
