package org.byteveda.agenteval.judge.multi;

import org.byteveda.agenteval.core.judge.JudgeResponse;

/**
 * The result from a single judge within a multi-model evaluation.
 *
 * @param modelId  the judge model identifier
 * @param response the judge's response (null if the judge failed)
 * @param weight   the weight assigned to this judge
 * @param error    the error message if the judge failed (null otherwise)
 */
public record IndividualJudgeResult(
        String modelId,
        JudgeResponse response,
        double weight,
        String error
) {

    /** Creates a successful result. */
    public static IndividualJudgeResult success(String modelId, JudgeResponse response,
                                                double weight) {
        return new IndividualJudgeResult(modelId, response, weight, null);
    }

    /** Creates a failed result. */
    public static IndividualJudgeResult failure(String modelId, double weight, String error) {
        return new IndividualJudgeResult(modelId, null, weight, error);
    }

    /** Returns true if this judge produced a successful response. */
    public boolean succeeded() {
        return response != null;
    }
}
