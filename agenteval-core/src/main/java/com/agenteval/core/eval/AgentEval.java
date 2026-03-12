package com.agenteval.core.eval;

import com.agenteval.core.config.AgentEvalConfig;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Static entry point for running evaluations.
 *
 * <pre>{@code
 * var results = AgentEval.evaluate(
 *     testCases,
 *     List.of(new AnswerRelevancy(0.7), new Faithfulness(0.8))
 * );
 * results.summary();
 * }</pre>
 */
public final class AgentEval {

    private static final Logger LOG = LoggerFactory.getLogger(AgentEval.class);

    private AgentEval() {}

    /**
     * Evaluates a list of test cases against the given metrics using default configuration.
     */
    public static EvalResult evaluate(List<AgentTestCase> testCases, List<EvalMetric> metrics) {
        return evaluate(testCases, metrics, AgentEvalConfig.defaults());
    }

    /**
     * Evaluates a list of test cases against the given metrics with the provided configuration.
     */
    public static EvalResult evaluate(List<AgentTestCase> testCases, List<EvalMetric> metrics,
                                      AgentEvalConfig config) {
        Objects.requireNonNull(testCases, "testCases must not be null");
        Objects.requireNonNull(metrics, "metrics must not be null");
        Objects.requireNonNull(config, "config must not be null");

        LOG.info("Starting evaluation: {} test cases, {} metrics", testCases.size(), metrics.size());
        long startTime = System.currentTimeMillis();

        List<CaseResult> caseResults = testCases.stream()
                .map(tc -> evaluateCase(tc, metrics))
                .toList();

        long durationMs = System.currentTimeMillis() - startTime;
        LOG.info("Evaluation complete in {}ms", durationMs);

        return EvalResult.of(caseResults, durationMs);
    }

    private static CaseResult evaluateCase(AgentTestCase testCase, List<EvalMetric> metrics) {
        Map<String, EvalScore> scores = new LinkedHashMap<>();
        boolean allPassed = true;

        for (EvalMetric metric : metrics) {
            try {
                EvalScore score = metric.evaluate(testCase);
                score = score.withMetricName(metric.name());
                scores.put(metric.name(), score);
                if (!score.passed()) {
                    allPassed = false;
                }
            } catch (Exception e) {
                LOG.error("Metric '{}' failed for test case: {}", metric.name(), e.getMessage(), e);
                EvalScore errorScore = EvalScore.fail("Metric evaluation error: " + e.getMessage())
                        .withMetricName(metric.name());
                scores.put(metric.name(), errorScore);
                allPassed = false;
            }
        }

        return new CaseResult(testCase, scores, allPassed);
    }

    /**
     * Creates a new configuration builder.
     */
    public static AgentEvalConfig.Builder configure() {
        return AgentEvalConfig.builder();
    }
}
