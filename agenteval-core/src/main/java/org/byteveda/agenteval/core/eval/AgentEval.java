package org.byteveda.agenteval.core.eval;

import org.byteveda.agenteval.core.config.AgentEvalConfig;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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

        List<CaseResult> caseResults;
        if (config.parallelEvaluation() && testCases.size() > 1) {
            caseResults = evaluateParallel(testCases, metrics, config, startTime);
        } else {
            caseResults = evaluateSequential(testCases, metrics, config, startTime);
        }

        long durationMs = System.currentTimeMillis() - startTime;
        LOG.info("Evaluation complete in {}ms", durationMs);

        return EvalResult.of(caseResults, durationMs);
    }

    private static List<CaseResult> evaluateSequential(
            List<AgentTestCase> testCases, List<EvalMetric> metrics,
            AgentEvalConfig config, long startTime) {
        ProgressCallback callback = config.progressCallback();
        int total = testCases.size();
        List<CaseResult> results = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            results.add(evaluateCase(testCases.get(i), metrics));
            if (callback != null) {
                callback.onProgress(buildProgressEvent(i + 1, total, startTime));
            }
        }
        return results;
    }

    private static List<CaseResult> evaluateParallel(
            List<AgentTestCase> testCases, List<EvalMetric> metrics,
            AgentEvalConfig config, long startTime) {
        int total = testCases.size();
        ProgressCallback callback = config.progressCallback();
        Semaphore semaphore = new Semaphore(config.parallelism());
        AtomicInteger completed = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<CaseResult>> futures = new ArrayList<>(total);

            for (AgentTestCase tc : testCases) {
                futures.add(executor.submit(() -> {
                    semaphore.acquire();
                    try {
                        CaseResult result = evaluateCase(tc, metrics);
                        int done = completed.incrementAndGet();
                        if (callback != null) {
                            callback.onProgress(buildProgressEvent(done, total, startTime));
                        }
                        return result;
                    } finally {
                        semaphore.release();
                    }
                }));
            }

            List<CaseResult> results = new ArrayList<>(total);
            for (Future<CaseResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    LOG.error("Parallel evaluation task failed", e);
                    throw new EvaluationException("Parallel evaluation failed", e);
                }
            }
            return results;
        }
    }

    private static ProgressEvent buildProgressEvent(int done, int total, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        long estimatedRemaining = -1;
        if (done > 0 && done < total) {
            long msPerCase = elapsed / done;
            estimatedRemaining = msPerCase * (total - done);
        } else if (done == total) {
            estimatedRemaining = 0;
        }
        return new ProgressEvent(done, total, elapsed, estimatedRemaining);
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
