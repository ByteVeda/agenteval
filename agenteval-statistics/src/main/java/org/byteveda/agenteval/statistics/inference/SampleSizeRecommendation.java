package org.byteveda.agenteval.statistics.inference;

/**
 * Recommendation for sample size based on observed effect size and desired power.
 *
 * @param currentSampleSize the current number of samples
 * @param recommendedSampleSize the recommended number of samples
 * @param desiredAlpha the significance level
 * @param desiredPower the desired statistical power (1 - beta)
 * @param observedEffectSize the observed effect size (Cohen's d)
 * @param rationale human-readable explanation of the recommendation
 */
public record SampleSizeRecommendation(
        int currentSampleSize,
        int recommendedSampleSize,
        double desiredAlpha,
        double desiredPower,
        double observedEffectSize,
        String rationale
) {
}
