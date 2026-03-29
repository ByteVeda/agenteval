package org.byteveda.agenteval.reporting.benchmark;

import org.byteveda.agenteval.core.benchmark.BenchmarkResult;
import org.byteveda.agenteval.core.eval.EvalResult;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;

/**
 * Reports benchmark results as a formatted console table with overall scores,
 * [BEST]/[WORST] labels, and per-metric breakdown.
 */
public final class BenchmarkReporter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BOLD = "\u001B[1m";

    private final PrintStream out;
    private final boolean ansiColors;

    public BenchmarkReporter() {
        this(System.out, true);
    }

    public BenchmarkReporter(PrintStream out, boolean ansiColors) {
        this.out = Objects.requireNonNull(out, "out must not be null");
        this.ansiColors = ansiColors;
    }

    /**
     * Reports benchmark results to the configured output stream.
     */
    public void report(BenchmarkResult result) {
        Objects.requireNonNull(result, "result must not be null");

        String best = result.bestVariant();
        String worst = result.worstVariant();
        boolean singleVariant = result.variantResults().size() == 1;

        printBold("=== Benchmark Results ===");
        out.printf("Variants: %d | Duration: %dms%n",
                result.variantResults().size(), result.totalDurationMs());
        out.println();

        // Overall scores table
        printBold("--- Overall Scores ---");
        out.printf("  %-30s %10s %10s  %s%n", "Variant", "Avg Score", "Pass Rate", "");
        for (Map.Entry<String, EvalResult> entry : result.variantResults().entrySet()) {
            String name = entry.getKey();
            EvalResult eval = entry.getValue();
            String label = "";
            if (!singleVariant) {
                if (name.equals(best)) {
                    label = colorize("[BEST]", ANSI_GREEN);
                } else if (name.equals(worst)) {
                    label = colorize("[WORST]", ANSI_RED);
                }
            }
            out.printf("  %-30s %10.3f %9.1f%%  %s%n",
                    name, eval.averageScore(), eval.passRate() * 100, label);
        }
        out.println();

        // Per-metric breakdown
        Map<String, Map<String, Double>> byMetric = result.scoresByMetric();
        if (!byMetric.isEmpty()) {
            printBold("--- Per-Metric Breakdown ---");
            for (Map.Entry<String, Map<String, Double>> metricEntry : byMetric.entrySet()) {
                out.printf("  %s:%n", metricEntry.getKey());
                for (Map.Entry<String, Double> variantScore : metricEntry.getValue().entrySet()) {
                    out.printf("    %-28s %.3f%n",
                            variantScore.getKey(), variantScore.getValue());
                }
            }
        }
    }

    private void printBold(String text) {
        if (ansiColors) {
            out.println(ANSI_BOLD + text + ANSI_RESET);
        } else {
            out.println(text);
        }
    }

    private String colorize(String text, String color) {
        if (ansiColors) {
            return color + text + ANSI_RESET;
        }
        return text;
    }
}
