package org.byteveda.agenteval.core.benchmark;

import org.byteveda.agenteval.core.eval.EvalResult;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregated results from a benchmark run across multiple variants.
 */
public final class BenchmarkResult {

    private final Map<String, EvalResult> variantResults;
    private final long totalDurationMs;

    public BenchmarkResult(Map<String, EvalResult> variantResults, long totalDurationMs) {
        Objects.requireNonNull(variantResults, "variantResults must not be null");
        this.variantResults = new LinkedHashMap<>(variantResults);
        this.totalDurationMs = totalDurationMs;
    }

    public Map<String, EvalResult> variantResults() {
        return Map.copyOf(variantResults);
    }

    public long totalDurationMs() { return totalDurationMs; }

    /**
     * Returns the result for a specific variant.
     *
     * @throws IllegalArgumentException if the variant name is unknown
     */
    public EvalResult resultFor(String variantName) {
        EvalResult result = variantResults.get(variantName);
        if (result == null) {
            throw new IllegalArgumentException("Unknown variant: " + variantName);
        }
        return result;
    }

    /**
     * Returns the variant name with the highest average score.
     */
    public String bestVariant() {
        return variantResults.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().averageScore()))
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalStateException("No variants"));
    }

    /**
     * Returns the variant name with the lowest average score.
     */
    public String worstVariant() {
        return variantResults.entrySet().stream()
                .min(Comparator.comparingDouble(e -> e.getValue().averageScore()))
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalStateException("No variants"));
    }

    /**
     * Returns variant names ordered by average score (descending).
     */
    public List<Map.Entry<String, Double>> averageScores() {
        return variantResults.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().averageScore()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();
    }

    /**
     * Returns per-metric scores grouped by metric name, then by variant.
     */
    public Map<String, Map<String, Double>> scoresByMetric() {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        for (Map.Entry<String, EvalResult> entry : variantResults.entrySet()) {
            String variant = entry.getKey();
            Map<String, Double> metricAvgs = entry.getValue().averageScoresByMetric();
            for (Map.Entry<String, Double> metricEntry : metricAvgs.entrySet()) {
                result.computeIfAbsent(metricEntry.getKey(), k -> new LinkedHashMap<>())
                        .put(variant, metricEntry.getValue());
            }
        }
        return result;
    }
}
