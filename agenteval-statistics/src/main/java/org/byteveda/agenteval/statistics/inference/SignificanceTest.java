package org.byteveda.agenteval.statistics.inference;

/**
 * Result of a statistical significance test.
 *
 * @param testName the name of the test (e.g., "Paired t-test", "Wilcoxon signed-rank")
 * @param testStatistic the computed test statistic
 * @param pValue the p-value
 * @param significant whether the result is significant at the given alpha
 * @param alpha the significance level used
 * @param interpretation human-readable interpretation of the result
 */
public record SignificanceTest(
        String testName,
        double testStatistic,
        double pValue,
        boolean significant,
        double alpha,
        String interpretation
) {
}
