package com.agenteval.reporting.regression;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Prints a human-readable regression comparison report.
 */
public final class RegressionReporter {

    private final PrintStream out;

    public RegressionReporter() {
        this(System.out);
    }

    public RegressionReporter(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    /**
     * Prints the regression report.
     */
    public void report(RegressionReport report) {
        Objects.requireNonNull(report, "report must not be null");

        out.println("=== Regression Report ===");
        out.printf("Overall: %.3f -> %.3f (%+.3f)%n",
                report.overallBaselineScore(),
                report.overallCurrentScore(),
                report.overallDelta());
        out.println();

        out.println("--- Per-Metric Changes ---");
        report.metricDeltas().forEach((name, delta) ->
                out.printf("  %-30s %.3f -> %.3f (%+.3f) %s%n",
                        name, delta.baselineScore(), delta.currentScore(),
                        delta.delta(),
                        delta.regressed() ? "[REGRESSED]"
                                : delta.improved() ? "[IMPROVED]" : ""));
        out.println();

        if (report.newFailures() > 0) {
            out.printf("New Failures: %d%n", report.newFailures());
            report.caseChanges().stream()
                    .filter(CaseStatusChange::newFailure)
                    .forEach(c -> out.printf("  - %s%n",
                            c.input().length() > 80
                                    ? c.input().substring(0, 80) + "..." : c.input()));
        }

        if (report.newPasses() > 0) {
            out.printf("New Passes: %d%n", report.newPasses());
            report.caseChanges().stream()
                    .filter(CaseStatusChange::newPass)
                    .forEach(c -> out.printf("  + %s%n",
                            c.input().length() > 80
                                    ? c.input().substring(0, 80) + "..." : c.input()));
        }

        out.println();
        out.println(report.hasRegressions()
                ? "RESULT: REGRESSIONS DETECTED"
                : "RESULT: NO REGRESSIONS");
    }
}
