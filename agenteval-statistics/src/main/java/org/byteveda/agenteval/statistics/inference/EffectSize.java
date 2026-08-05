package org.byteveda.agenteval.statistics.inference;

/**
 * Effect size measurement using Cohen's d.
 *
 * @param cohensD the Cohen's d value
 * @param magnitude the qualitative magnitude classification
 */
public record EffectSize(double cohensD, Magnitude magnitude) {

    /**
     * Qualitative magnitude classification for effect sizes based on Cohen's conventions.
     */
    public enum Magnitude {
        /** |d| &lt; 0.2 */
        NEGLIGIBLE,
        /** 0.2 &lt;= |d| &lt; 0.5 */
        SMALL,
        /** 0.5 &lt;= |d| &lt; 0.8 */
        MEDIUM,
        /** |d| &gt;= 0.8 */
        LARGE
    }

    /**
     * Classifies the magnitude of a Cohen's d value.
     *
     * @param d the Cohen's d value
     * @return the magnitude classification
     */
    public static Magnitude classify(double d) {
        double absD = Math.abs(d);
        if (absD < 0.2) {
            return Magnitude.NEGLIGIBLE;
        } else if (absD < 0.5) {
            return Magnitude.SMALL;
        } else if (absD < 0.8) {
            return Magnitude.MEDIUM;
        } else {
            return Magnitude.LARGE;
        }
    }
}
