package org.byteveda.agenteval.statistics.math;

import java.util.random.RandomGenerator;

/**
 * Bootstrap resampling engine for non-parametric confidence intervals.
 *
 * <p>All methods are pure functions (given a seeded RNG) with no side effects.</p>
 *
 * <p><strong>Internal API:</strong> This class is intended for use within the
 * agenteval-statistics module only. It is not part of the public API and may
 * change without notice.</p>
 */
public final class BootstrapSampler {

    private BootstrapSampler() {
        // utility class
    }

    /**
     * Generates bootstrap sample means by resampling with replacement.
     *
     * @param data the original data array (must not be empty)
     * @param iterations the number of bootstrap iterations
     * @param rng the random number generator to use for reproducibility
     * @return array of bootstrap sample means, sorted in ascending order
     * @throws IllegalArgumentException if data is empty or iterations is non-positive
     */
    public static double[] bootstrapMeans(double[] data, int iterations, RandomGenerator rng) {
        if (data.length == 0) {
            throw new IllegalArgumentException("data must not be empty");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive, got: " + iterations);
        }

        int n = data.length;
        double[] means = new double[iterations];

        for (int i = 0; i < iterations; i++) {
            double sum = 0.0;
            for (int j = 0; j < n; j++) {
                sum += data[rng.nextInt(n)];
            }
            means[i] = sum / n;
        }

        java.util.Arrays.sort(means);
        return means;
    }
}
