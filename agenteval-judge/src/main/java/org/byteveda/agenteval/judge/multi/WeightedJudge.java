package org.byteveda.agenteval.judge.multi;

import org.byteveda.agenteval.core.judge.JudgeModel;

/**
 * A judge model paired with a weight for weighted consensus strategies.
 *
 * @param model  the judge model instance
 * @param weight the relative weight (must be positive)
 */
public record WeightedJudge(JudgeModel model, double weight) {

    public WeightedJudge {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        if (weight <= 0.0) {
            throw new IllegalArgumentException("weight must be positive, got: " + weight);
        }
    }
}
