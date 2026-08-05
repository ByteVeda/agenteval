package org.byteveda.agenteval.statistics.inference;

/**
 * Result of a normality test for a metric's score distribution.
 *
 * @param metricName the metric being tested
 * @param statistic the test statistic value
 * @param pValue the p-value (high p-value suggests normality)
 * @param isNormal whether the distribution appears normal at the significance level
 * @param testName the name of the normality test used
 */
public record NormalityTest(
        String metricName,
        double statistic,
        double pValue,
        boolean isNormal,
        String testName
) {
}
