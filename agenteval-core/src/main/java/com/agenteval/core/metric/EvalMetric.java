package com.agenteval.core.metric;

import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;

/**
 * Interface for all evaluation metrics.
 *
 * <p>Implementations evaluate an {@link AgentTestCase} and return an {@link EvalScore}
 * normalized to the 0.0–1.0 range. Metrics must be thread-safe.</p>
 */
public interface EvalMetric {

    /**
     * Evaluates the given test case and returns a score.
     *
     * @param testCase the test case to evaluate
     * @return the evaluation score (0.0–1.0)
     */
    EvalScore evaluate(AgentTestCase testCase);

    /**
     * Returns the name of this metric.
     */
    String name();
}
