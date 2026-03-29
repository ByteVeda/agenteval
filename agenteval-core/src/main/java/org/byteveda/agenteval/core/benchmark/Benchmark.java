package org.byteveda.agenteval.core.benchmark;

import org.byteveda.agenteval.core.eval.AgentEval;
import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.core.eval.EvaluationException;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * Runs the same dataset against multiple configuration variants and collects results.
 *
 * <pre>{@code
 * var result = Benchmark.run(testCases, List.of(variantA, variantB));
 * System.out.println("Best: " + result.bestVariant());
 * }</pre>
 */
public final class Benchmark {

    private static final Logger LOG = LoggerFactory.getLogger(Benchmark.class);

    private Benchmark() {}

    /**
     * Runs benchmarks sequentially with default config.
     */
    public static BenchmarkResult run(List<AgentTestCase> testCases,
                                       List<BenchmarkVariant> variants) {
        return run(testCases, variants, BenchmarkConfig.defaults());
    }

    /**
     * Runs benchmarks with the specified config.
     */
    public static BenchmarkResult run(List<AgentTestCase> testCases,
                                       List<BenchmarkVariant> variants,
                                       BenchmarkConfig config) {
        Objects.requireNonNull(testCases, "testCases must not be null");
        Objects.requireNonNull(variants, "variants must not be null");
        Objects.requireNonNull(config, "config must not be null");

        validateUniqueNames(variants);

        long startTime = System.currentTimeMillis();
        Map<String, EvalResult> results;

        if (config.parallelVariants() && variants.size() > 1) {
            results = runParallel(testCases, variants, config);
        } else {
            results = runSequential(testCases, variants);
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        LOG.info("Benchmark complete: {} variants in {}ms", variants.size(), totalDuration);

        return new BenchmarkResult(results, totalDuration);
    }

    private static Map<String, EvalResult> runSequential(
            List<AgentTestCase> testCases, List<BenchmarkVariant> variants) {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        for (BenchmarkVariant variant : variants) {
            LOG.info("Running variant '{}'", variant.name());
            results.put(variant.name(), evaluateVariant(testCases, variant));
        }
        return results;
    }

    private static Map<String, EvalResult> runParallel(
            List<AgentTestCase> testCases, List<BenchmarkVariant> variants,
            BenchmarkConfig config) {
        Semaphore semaphore = new Semaphore(config.maxParallelVariants());
        Map<String, EvalResult> results = new LinkedHashMap<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Map.Entry<String, EvalResult>>> futures = new ArrayList<>();

            for (BenchmarkVariant variant : variants) {
                futures.add(executor.submit(() -> {
                    semaphore.acquire();
                    try {
                        LOG.info("Running variant '{}' (parallel)", variant.name());
                        EvalResult result = evaluateVariant(testCases, variant);
                        return Map.entry(variant.name(), result);
                    } finally {
                        semaphore.release();
                    }
                }));
            }

            for (Future<Map.Entry<String, EvalResult>> future : futures) {
                try {
                    Map.Entry<String, EvalResult> entry = future.get();
                    results.put(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    throw new EvaluationException("Parallel benchmark failed", e);
                }
            }
        }

        return results;
    }

    private static EvalResult evaluateVariant(List<AgentTestCase> testCases,
                                               BenchmarkVariant variant) {
        // Deep-copy test cases for this variant to ensure isolation
        List<AgentTestCase> copied = testCases.stream()
                .map(tc -> tc.toBuilder().build())
                .map(variant.casePreparer())
                .toList();

        return AgentEval.evaluate(copied, variant.metrics(), variant.config());
    }

    private static void validateUniqueNames(List<BenchmarkVariant> variants) {
        Set<String> seen = new HashSet<>();
        for (BenchmarkVariant v : variants) {
            if (!seen.add(v.name())) {
                throw new IllegalArgumentException("Duplicate variant name: " + v.name());
            }
        }
    }
}
