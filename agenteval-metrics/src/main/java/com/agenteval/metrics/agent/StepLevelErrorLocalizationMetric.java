package com.agenteval.metrics.agent;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import com.agenteval.core.model.ReasoningStep;
import com.agenteval.core.template.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * LLM-as-judge metric that localizes the first error in an agent's reasoning trace.
 *
 * <p>Iterates through each reasoning step and uses the judge to determine correctness.
 * Score = (index of first failing step / total steps), or 1.0 if all steps pass.
 * Higher score = error occurred later (more of the reasoning was correct).</p>
 */
public final class StepLevelErrorLocalizationMetric implements EvalMetric {

    private static final Logger LOG = LoggerFactory.getLogger(
            StepLevelErrorLocalizationMetric.class);
    private static final String NAME = "StepLevelErrorLocalization";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/step-error-localization.txt";
    private static final double DEFAULT_THRESHOLD = 0.7;
    private static final double STEP_PASS_THRESHOLD = 0.5;

    private final JudgeModel judge;
    private final double threshold;

    public StepLevelErrorLocalizationMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD);
    }

    public StepLevelErrorLocalizationMetric(JudgeModel judge, double threshold) {
        this.judge = Objects.requireNonNull(judge, "judge must not be null");
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        this.threshold = threshold;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        List<ReasoningStep> trace = testCase.getReasoningTrace();
        if (trace.isEmpty()) {
            return EvalScore.of(1.0, threshold, "No reasoning trace to evaluate");
        }

        if (testCase.getInput() == null || testCase.getInput().isBlank()) {
            throw new IllegalArgumentException(NAME + " requires non-empty input");
        }

        int totalSteps = trace.size();
        for (int i = 0; i < totalSteps; i++) {
            ReasoningStep step = trace.get(i);
            Map<String, String> vars = new HashMap<>();
            vars.put("input", testCase.getInput());
            vars.put("expectedOutput", testCase.getExpectedOutput() != null
                    ? testCase.getExpectedOutput() : "(none)");
            vars.put("stepIndex", String.valueOf(i + 1));
            vars.put("totalSteps", String.valueOf(totalSteps));
            vars.put("stepType", step.type().name());
            vars.put("stepContent", step.content());
            vars.put("previousSteps", formatPreviousSteps(trace, i));

            String prompt = PromptTemplate.loadAndRender(PROMPT_PATH, vars);
            JudgeResponse response = judge.judge(prompt);

            LOG.debug("Step {}/{} scored {}: {}",
                    i + 1, totalSteps, response.score(), response.reason());

            if (response.score() < STEP_PASS_THRESHOLD) {
                double score = (double) i / totalSteps;
                String reason = String.format(
                        "First error at step %d/%d (%s): %s",
                        i + 1, totalSteps, step.type().name(), response.reason());
                return EvalScore.of(score, threshold, reason);
            }
        }

        return EvalScore.of(1.0, threshold, "All " + totalSteps + " steps are correct");
    }

    @Override
    public String name() {
        return NAME;
    }

    private static String formatPreviousSteps(List<ReasoningStep> trace, int upTo) {
        if (upTo == 0) return "(none)";
        var sb = new StringBuilder();
        for (int i = 0; i < upTo; i++) {
            ReasoningStep s = trace.get(i);
            sb.append("Step ").append(i + 1).append(" [").append(s.type().name())
                    .append("]: ").append(s.content());
            if (i < upTo - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
