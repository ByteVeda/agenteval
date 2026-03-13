package com.agenteval.reporting.benchmark;

import com.agenteval.core.benchmark.BenchmarkResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.reporting.regression.RegressionComparison;
import com.agenteval.reporting.regression.RegressionReport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Static utility for comparing benchmark variants using regression analysis.
 */
public final class BenchmarkComparison {

    private BenchmarkComparison() {}

    /**
     * Compares two variants within a benchmark result.
     *
     * @param result     the benchmark result
     * @param baseline   the baseline variant name
     * @param comparison the comparison variant name
     * @return the regression report
     * @throws IllegalArgumentException if either variant name is unknown
     */
    public static RegressionReport compareVariants(BenchmarkResult result,
                                                    String baseline,
                                                    String comparison) {
        Objects.requireNonNull(result, "result must not be null");
        EvalResult baseResult = result.resultFor(baseline);
        EvalResult compResult = result.resultFor(comparison);
        return RegressionComparison.compare(baseResult, compResult);
    }

    /**
     * Compares all variants against a baseline variant.
     *
     * @param result   the benchmark result
     * @param baseline the baseline variant name
     * @return map of variant name to regression report (excludes baseline)
     * @throws IllegalArgumentException if the baseline variant name is unknown
     */
    public static Map<String, RegressionReport> compareAllAgainst(BenchmarkResult result,
                                                                    String baseline) {
        Objects.requireNonNull(result, "result must not be null");
        EvalResult baseResult = result.resultFor(baseline);
        Map<String, RegressionReport> reports = new LinkedHashMap<>();

        for (Map.Entry<String, EvalResult> entry : result.variantResults().entrySet()) {
            if (!entry.getKey().equals(baseline)) {
                reports.put(entry.getKey(),
                        RegressionComparison.compare(baseResult, entry.getValue()));
            }
        }
        return reports;
    }
}
