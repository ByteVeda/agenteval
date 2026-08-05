package org.byteveda.agenteval.statistics.comparison;

import org.byteveda.agenteval.reporting.regression.RegressionReport;
import org.byteveda.agenteval.statistics.inference.EffectSize;
import org.byteveda.agenteval.statistics.inference.SignificanceTest;

import java.util.List;
import java.util.Map;

/**
 * Enhanced regression report that wraps the base {@link RegressionReport} with
 * statistical significance testing, effect sizes, and per-metric comparisons.
 *
 * @param baseReport the original regression report
 * @param overallSignificance overall significance test result
 * @param overallEffectSize overall effect size
 * @param metricComparisons per-metric statistical comparisons
 * @param warnings statistical warnings and caveats
 */
public record EnhancedRegressionReport(
        RegressionReport baseReport,
        SignificanceTest overallSignificance,
        EffectSize overallEffectSize,
        Map<String, StatisticalComparison> metricComparisons,
        List<String> warnings
) {
    /**
     * Compact constructor ensuring immutable collections.
     */
    public EnhancedRegressionReport {
        metricComparisons = Map.copyOf(metricComparisons);
        warnings = List.copyOf(warnings);
    }

    /**
     * Returns true if the overall difference is statistically significant.
     *
     * @return true if the overall significance test reports significance
     */
    public boolean isSignificant() {
        return overallSignificance.significant();
    }

    /**
     * Returns true if there are statistically significant regressions.
     *
     * @return true if both regression detected and statistically significant
     */
    public boolean hasSignificantRegressions() {
        return baseReport.hasRegressions() && isSignificant();
    }
}
