package com.agenteval.junit5.extension;

import com.agenteval.core.config.AgentEvalConfig;
import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
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
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 5 extension that provides {@link AgentTestCase} parameter resolution,
 * captures test case instances via invocation interception, and evaluates
 * declared {@link Metric} annotations after each test.
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
        return parameterContext.getParameter().getType() == AgentTestCase.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) {
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

        JudgeModel judgeModel = resolveJudgeModel();
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

    private JudgeModel resolveJudgeModel() {
        if (config != null) {
            return config.judgeModel();
        }
        return null;
    }
}
