package com.agenteval.judge.multi;

import com.agenteval.core.judge.JudgeResponse;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated response from a multi-model judge evaluation.
 *
 * @param consensusResponse  the aggregated consensus response
 * @param individualResults  results from each individual judge
 */
public record MultiJudgeResponse(
        JudgeResponse consensusResponse,
        List<IndividualJudgeResult> individualResults
) {

    public MultiJudgeResponse {
        Objects.requireNonNull(consensusResponse, "consensusResponse must not be null");
        individualResults = individualResults == null
                ? List.of() : List.copyOf(individualResults);
    }

    /** Returns only the successful individual results. */
    public List<IndividualJudgeResult> successfulResults() {
        return individualResults.stream()
                .filter(IndividualJudgeResult::succeeded)
                .toList();
    }

    /** Returns only the failed individual results. */
    public List<IndividualJudgeResult> failedResults() {
        return individualResults.stream()
                .filter(r -> !r.succeeded())
                .toList();
    }
}
