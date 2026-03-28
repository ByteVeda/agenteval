package com.agenteval.junit5.extension;

import com.agenteval.core.config.AgentEvalConfig;
import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import com.agenteval.datasets.DatasetLoaders;
import com.agenteval.datasets.EvalDataset;
import com.agenteval.junit5.annotation.EvalTimeout;
import com.agenteval.junit5.annotation.GoldenSet;
import com.agenteval.junit5.annotation.JudgeModelConfig;
import com.agenteval.junit5.annotation.Metric;
import com.agenteval.junit5.annotation.Metrics;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * JUnit 5 extension that provides {@link AgentTestCase} parameter resolution,
 * captures test case instances via invocation interception, and evaluates
 * declared {@link Metric} annotations after each test.
 *
 * <p>Supports {@link GoldenSet} parameter injection, {@link JudgeModelConfig}
 * overrides, and {@link EvalTimeout} for time-limited evaluations.</p>
 *
 * <p>Registered automatically via {@code @AgentTest}, {@code @Metric},
 * or {@code @Metrics} meta-annotations. Can also be registered explicitly:</p>
 *
 * <pre>{@code
 * @RegisterExtension
 * static AgentEvalExtension ext = AgentEvalExtension.withConfig(config);
 * }</pre>
 */
public class AgentEvalExtension
        implements ParameterResolver, InvocationInterceptor, AfterEachCallback {

    private static final Logger LOG = LoggerFactory.getLogger(AgentEvalExtension.class);
    private static final Namespace NS = Namespace.create(AgentEvalExtension.class);
    private static final String TEST_CASE_KEY = "agentTestCase";

    private final AgentEvalConfig config;

    /**
     * Default constructor used by {@code @ExtendWith} (via meta-annotations).
     */
    public AgentEvalExtension() {
        this(null);
    }

    private AgentEvalExtension(AgentEvalConfig config) {
        this.config = config;
    }

    /**
     * Creates an extension with explicit configuration (for {@code @RegisterExtension}).
     */
    public static AgentEvalExtension withConfig(AgentEvalConfig config) {
        return new AgentEvalExtension(config);
    }

    // --- ParameterResolver ---

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        Parameter param = parameterContext.getParameter();
        if (param.getType() == AgentTestCase.class) {
            return true;
        }
        if (param.isAnnotationPresent(GoldenSet.class)) {
            return param.getType() == EvalDataset.class
                    || param.getType() == List.class;
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        Parameter param = parameterContext.getParameter();

        // Handle @GoldenSet injection
        if (param.isAnnotationPresent(GoldenSet.class)) {
            GoldenSet goldenSet = param.getAnnotation(GoldenSet.class);
            EvalDataset dataset = DatasetLoaders.forPath(Path.of(goldenSet.value()));
            if (param.getType() == List.class) {
                return dataset.getTestCases();
            }
            return dataset;
        }

        // Default: resolve AgentTestCase
        var store = extensionContext.getStore(NS);
        AgentTestCase testCase = store.get(TEST_CASE_KEY, AgentTestCase.class);
        if (testCase == null) {
            testCase = AgentTestCase.builder().input("").build();
            store.put(TEST_CASE_KEY, testCase);
        }
        return testCase;
    }

    // --- InvocationInterceptor ---

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        captureTestCase(invocationContext, extensionContext);
        invocation.proceed();
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        captureTestCase(invocationContext, extensionContext);
        invocation.proceed();
    }

    private void captureTestCase(ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) {
        for (Object arg : invocationContext.getArguments()) {
            if (arg instanceof AgentTestCase tc) {
                extensionContext.getStore(NS).put(TEST_CASE_KEY, tc);
                return;
            }
        }
    }

    // --- AfterEachCallback ---

    @Override
    public void afterEach(ExtensionContext context) {
        List<Metric> metricAnnotations = findMetricAnnotations(context);
        if (metricAnnotations.isEmpty()) return;

        AgentTestCase testCase = context.getStore(NS).get(TEST_CASE_KEY, AgentTestCase.class);
        if (testCase == null) {
            LOG.warn("No AgentTestCase found in store; skipping metric evaluation");
            return;
        }

        JudgeModel judgeModel = resolveJudgeModel(context);

        // Check for @EvalTimeout
        EvalTimeout timeout = context.getTestMethod()
                .map(m -> m.getAnnotation(EvalTimeout.class))
                .orElse(null);

        if (timeout != null) {
            evaluateWithTimeout(metricAnnotations, testCase, judgeModel,
                    timeout.value(), timeout.unit());
        } else {
            evaluateMetrics(metricAnnotations, testCase, judgeModel);
        }
    }

    private void evaluateWithTimeout(List<Metric> metricAnnotations,
            AgentTestCase testCase, JudgeModel judgeModel,
            long duration, TimeUnit unit) {
        try {
            CompletableFuture.runAsync(() ->
                    evaluateMetrics(metricAnnotations, testCase, judgeModel)
            ).get(duration, unit);
        } catch (TimeoutException e) {
            throw new AssertionError(String.format(
                    "Metric evaluation timed out after %d %s", duration, unit));
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AssertionError ae) {
                throw ae;
            }
            throw new AssertionError("Metric evaluation failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Metric evaluation interrupted", e);
        }
    }

    private void evaluateMetrics(List<Metric> metricAnnotations,
            AgentTestCase testCase, JudgeModel judgeModel) {
        List<String> failures = new ArrayList<>();

        for (Metric metricAnn : metricAnnotations) {
            EvalMetric metric = MetricFactory.create(
                    metricAnn.value(), metricAnn.threshold(), judgeModel);
            EvalScore score = metric.evaluate(testCase);
            score = score.withMetricName(metric.name());

            LOG.debug("Metric '{}': score={}, threshold={}, passed={}",
                    metric.name(), score.value(), score.threshold(), score.passed());

            if (!score.passed()) {
                failures.add(String.format("%s: %.3f < %.3f (%s)",
                        metric.name(), score.value(), score.threshold(), score.reason()));
            }
        }

        if (!failures.isEmpty()) {
            throw new AssertionError("Metric evaluation failed:\n  "
                    + String.join("\n  ", failures));
        }
    }

    private List<Metric> findMetricAnnotations(ExtensionContext context) {
        return context.getTestMethod()
                .map(method -> {
                    List<Metric> result = new ArrayList<>();
                    // Direct @Metric annotations
                    Metric single = method.getAnnotation(Metric.class);
                    if (single != null) {
                        result.add(single);
                    }
                    // @Metrics container
                    Metrics container = method.getAnnotation(Metrics.class);
                    if (container != null) {
                        result.addAll(List.of(container.value()));
                    }
                    return result;
                })
                .orElse(List.of());
    }

    /**
     * Resolves the judge model, checking for {@code @JudgeModelConfig} override
     * on the method first, then on the class, then falling back to the config.
     */
    private JudgeModel resolveJudgeModel(ExtensionContext context) {
        // Check method-level @JudgeModelConfig
        JudgeModelConfig methodLevel = context.getTestMethod()
                .map(m -> m.getAnnotation(JudgeModelConfig.class))
                .orElse(null);
        if (methodLevel != null) {
            return createJudgeFromAnnotation(methodLevel);
        }

        // Check class-level @JudgeModelConfig
        JudgeModelConfig classLevel = context.getTestClass()
                .map(c -> c.getAnnotation(JudgeModelConfig.class))
                .orElse(null);
        if (classLevel != null) {
            return createJudgeFromAnnotation(classLevel);
        }

        // Fall back to config
        if (config != null) {
            return config.judgeModel();
        }
        return null;
    }

    private JudgeModel createJudgeFromAnnotation(JudgeModelConfig annotation) {
        String apiKey = annotation.apiKey().isEmpty()
                ? resolveApiKeyFromEnv(annotation.provider())
                : annotation.apiKey();

        return JudgeModelResolver.resolve(annotation.provider(), annotation.model(), apiKey);
    }

    private String resolveApiKeyFromEnv(String provider) {
        return switch (provider.toLowerCase()) {
            case "openai" -> System.getenv("OPENAI_API_KEY");
            case "anthropic" -> System.getenv("ANTHROPIC_API_KEY");
            case "google" -> System.getenv("GOOGLE_API_KEY");
            case "azure" -> System.getenv("AZURE_OPENAI_API_KEY");
            default -> null;
        };
    }
}
