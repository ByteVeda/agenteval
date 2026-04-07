package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.template.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Uses an LLM judge to evaluate how well an agent handled a chaos scenario.
 *
 * <p>The evaluation prompt is loaded from a classpath resource at
 * {@code com/agenteval/chaos/prompts/resilience-evaluation.txt}.</p>
 */
public final class ResilienceEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(ResilienceEvaluator.class);

    private static final String PROMPT_RESOURCE =
            "com/agenteval/chaos/prompts/resilience-evaluation.txt";

    private final JudgeModel judge;

    /**
     * Creates an evaluator backed by the given judge model.
     *
     * @param judge the LLM judge to use for evaluation
     */
    public ResilienceEvaluator(JudgeModel judge) {
        this.judge = Objects.requireNonNull(judge, "judge must not be null");
    }

    /**
     * Evaluates how well the agent handled a chaos scenario.
     *
     * @param scenario the chaos scenario that was applied
     * @param agentInput the input that was sent to the agent
     * @param agentResponse the agent's response
     * @return the judge's evaluation with score and reasoning
     */
    public JudgeResponse evaluate(ChaosScenario scenario, String agentInput,
            String agentResponse) {
        String prompt = PromptTemplate.loadAndRender(PROMPT_RESOURCE, Map.of(
                "failureType", scenario.category().name(),
                "failureDescription", scenario.description(),
                "input", agentInput,
                "response", agentResponse != null ? agentResponse : "(no response)"
        ));

        LOG.debug("Evaluating resilience for scenario: {} [{}]",
                scenario.name(), scenario.category());
        return judge.judge(prompt);
    }
}
