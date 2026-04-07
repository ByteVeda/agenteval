package org.byteveda.agenteval.fingerprint;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;

/**
 * Prints capability profiles and comparison results to the console.
 */
public final class CapabilityReporter {

    private static final String HORIZONTAL_RULE =
            "+----------------------+-------+----------------------------------------+";
    private static final String HEADER_FORMAT = "| %-20s | %5s | %-38s |%n";
    private static final String ROW_FORMAT = "| %-20s | %5.3f | %-38s |%n";

    private CapabilityReporter() {}

    /**
     * Prints a capability profile as a formatted table to stdout.
     *
     * @param profile the profile to print
     */
    public static void printProfile(CapabilityProfile profile) {
        printProfile(profile, System.out);
    }

    /**
     * Prints a capability profile as a formatted table.
     *
     * @param profile the profile to print
     * @param out     the output stream
     */
    public static void printProfile(CapabilityProfile profile, PrintStream out) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(out, "out must not be null");

        out.println();
        out.printf("=== Capability Profile: %s ===%n", profile.agentName());
        out.printf("Overall Score: %.3f | Duration: %dms%n",
                profile.overallScore(), profile.durationMs());
        out.println();
        out.println(HORIZONTAL_RULE);
        out.printf(HEADER_FORMAT, "Dimension", "Score", "Reason");
        out.println(HORIZONTAL_RULE);

        for (Map.Entry<CapabilityDimension, ProfileScore> entry
                : profile.scores().entrySet()) {
            ProfileScore score = entry.getValue();
            String reason = truncate(score.reason(), 38);
            out.printf(ROW_FORMAT, score.dimension().displayName(),
                    score.score(), reason);
        }

        out.println(HORIZONTAL_RULE);

        if (!profile.strengths().isEmpty()) {
            out.print("Strengths: ");
            out.println(profile.strengths().stream()
                    .map(CapabilityDimension::displayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
        }
        if (!profile.weaknesses().isEmpty()) {
            out.print("Weaknesses: ");
            out.println(profile.weaknesses().stream()
                    .map(CapabilityDimension::displayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
        }
        out.println();
    }

    /**
     * Prints a comparison result as a formatted table to stdout.
     *
     * @param result the comparison result
     */
    public static void printComparison(CapabilityComparisonResult result) {
        printComparison(result, System.out);
    }

    /**
     * Prints a comparison result as a formatted table.
     *
     * @param result the comparison result
     * @param out    the output stream
     */
    public static void printComparison(CapabilityComparisonResult result,
            PrintStream out) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(out, "out must not be null");

        String comparisonRule =
                "+----------------------+-------+-------+--------+";
        String comparisonHeader = "| %-20s | %5s | %5s | %6s |%n";
        String comparisonRow = "| %-20s | %5.3f | %5.3f | %+6.3f |%n";

        out.println();
        out.printf("=== Comparison: %s vs %s ===%n",
                result.profileA().agentName(),
                result.profileB().agentName());
        out.printf("Overall Delta: %+.3f%n", result.overallDelta());
        out.println();
        out.println(comparisonRule);
        out.printf(comparisonHeader, "Dimension", "A", "B", "Delta");
        out.println(comparisonRule);

        for (Map.Entry<CapabilityDimension, Double> entry
                : result.deltas().entrySet()) {
            CapabilityDimension dim = entry.getKey();
            double delta = entry.getValue();
            double scoreA = getScore(result.profileA(), dim);
            double scoreB = getScore(result.profileB(), dim);
            out.printf(comparisonRow, dim.displayName(), scoreA, scoreB, delta);
        }

        out.println(comparisonRule);

        if (!result.improvements().isEmpty()) {
            out.print("Improvements: ");
            out.println(result.improvements().stream()
                    .map(CapabilityDimension::displayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
        }
        if (!result.regressions().isEmpty()) {
            out.print("Regressions: ");
            out.println(result.regressions().stream()
                    .map(CapabilityDimension::displayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
        }
        out.println();
    }

    private static double getScore(CapabilityProfile profile,
            CapabilityDimension dimension) {
        ProfileScore score = profile.scores().get(dimension);
        return score != null ? score.score() : 0.0;
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
