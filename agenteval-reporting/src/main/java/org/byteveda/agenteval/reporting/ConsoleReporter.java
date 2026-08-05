package org.byteveda.agenteval.reporting;

import org.byteveda.agenteval.core.eval.CaseResult;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.model.EvalScore;

import java.io.PrintStream;
import java.util.Map;

/**
 * Reports evaluation results as a formatted table to a {@link PrintStream}.
 *
 * <p>Supports ANSI color output for terminal display.</p>
 */
public final class ConsoleReporter implements EvalReporter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BOLD = "\u001B[1m";

    private final PrintStream out;
    private final boolean ansiColors;

    public ConsoleReporter() {
        this(System.out, true);
    }

    public ConsoleReporter(PrintStream out, boolean ansiColors) {
        this.out = out;
        this.ansiColors = ansiColors;
    }

    @Override
    public void report(EvalResult result) {
        int total = result.caseResults().size();
        int failed = result.failedCases().size();
        int passed = total - failed;

        printBold("=== AgentEval Results ===");
        out.printf("Cases: %d | Passed: %d | Failed: %d | Pass Rate: %.1f%%%n",
                total, passed, failed, result.passRate() * 100);
        out.printf("Average Score: %.3f | Duration: %dms%n",
                result.averageScore(), result.durationMs());

        Map<String, Double> byMetric = result.averageScoresByMetric();
        if (!byMetric.isEmpty()) {
            out.println("--- Per-Metric Averages ---");
            byMetric.forEach((name, avg) -> {
                String icon = avg >= 0.5 ? passIcon() : failIcon();
                out.printf("  %-30s %.3f  %s%n", name, avg, icon);
            });
        }

        if (!result.failedCases().isEmpty()) {
            out.println("--- Failed Cases ---");
            for (CaseResult cr : result.failedCases()) {
                String input = truncate(cr.testCase().getInput(), 40);
                for (EvalScore score : cr.failedScores()) {
                    out.printf("  Case \"%s\" %s %s: %.2f < %.2f (%s)%n",
                            input,
                            ansiColors ? ANSI_RED : "",
                            score.metricName(),
                            score.value(),
                            score.threshold(),
                            score.reason() + (ansiColors ? ANSI_RESET : ""));
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

    private String passIcon() {
        if (ansiColors) {
            return ANSI_GREEN + "\u2713" + ANSI_RESET;
        }
        return "PASS";
    }

    private String failIcon() {
        if (ansiColors) {
            return ANSI_RED + "\u2717" + ANSI_RESET;
        }
        return "FAIL";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
