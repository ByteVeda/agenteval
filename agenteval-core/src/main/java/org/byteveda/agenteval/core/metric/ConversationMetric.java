package org.byteveda.agenteval.core.metric;

import org.byteveda.agenteval.core.model.ConversationTestCase;
import org.byteveda.agenteval.core.model.EvalScore;

/**
 * Interface for conversation-level evaluation metrics.
 *
 * <p>Unlike {@link EvalMetric} which evaluates single-turn {@code AgentTestCase},
 * this interface evaluates multi-turn {@link ConversationTestCase} instances.
 * Implementations must be thread-safe.</p>
 */
public interface ConversationMetric {

    /**
     * Evaluates the given conversation test case and returns a score.
     *
     * @param testCase the multi-turn conversation to evaluate
     * @return the evaluation score (0.0-1.0)
     */
    EvalScore evaluate(ConversationTestCase testCase);

    /**
     * Returns the name of this metric.
     */
    String name();
}
