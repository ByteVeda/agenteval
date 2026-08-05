package org.byteveda.agenteval.mutation;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated result of running a full mutation test suite.
 *
 * @param results    individual mutation results
 * @param durationMs total time in milliseconds
 */
public record MutationSuiteResult(
        List<MutationResult> results,
        long durationMs
) {

    public MutationSuiteResult {
        Objects.requireNonNull(results, "results must not be null");
        results = List.copyOf(results);
    }

    /**
     * Returns only the mutations that were <em>not</em> detected by the evaluation.
     *
     * <p>A high count of undetected mutations indicates that either the prompt
     * instructions are redundant or the evaluation metrics lack sensitivity.</p>
     *
     * @return undetected mutation results
     */
    public List<MutationResult> undetectedMutations() {
        return results.stream()
                .filter(r -> !r.detected())
                .toList();
    }

    /**
     * Returns the mutation detection rate (0.0 to 1.0).
     *
     * @return the fraction of mutations that were detected
     */
    public double detectionRate() {
        if (results.isEmpty()) {
            return 0.0;
        }
        long detected = results.stream().filter(MutationResult::detected).count();
        return (double) detected / results.size();
    }

    /**
     * Returns the total number of mutations that were tested.
     */
    public int totalMutations() {
        return results.size();
    }

    /**
     * Returns the number of mutations that were detected.
     */
    public int detectedCount() {
        return (int) results.stream().filter(MutationResult::detected).count();
    }
}
