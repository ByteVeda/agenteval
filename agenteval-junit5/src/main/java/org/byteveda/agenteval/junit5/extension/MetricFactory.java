package org.byteveda.agenteval.junit5.extension;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.metric.EvalMetric;
import java.lang.reflect.Constructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reflectively instantiates {@link EvalMetric} implementations from annotation metadata.
 *
 * <p>Tries constructors in order:</p>
 * <ol>
 *   <li>{@code (JudgeModel, double)} — LLM metric with explicit threshold</li>
 *   <li>{@code (double)} — deterministic metric with threshold</li>
 *   <li>{@code (JudgeModel)} — LLM metric with default threshold</li>
 *   <li>{@code ()} — no-arg (deterministic metrics)</li>
 * </ol>
 *
 * <p>A threshold of {@code -1.0} means "use metric default" — the threshold parameter
 * is skipped and constructors without a threshold parameter are preferred.</p>
 */
public final class MetricFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MetricFactory.class);
    private static final double USE_DEFAULT_THRESHOLD = -1.0;

    private MetricFactory() {}

    /**
     * Creates a metric instance from its class and configuration.
     *
     * @param metricClass the metric class
     * @param threshold the threshold (-1.0 for metric default)
     * @param judgeModel the judge model (may be null for deterministic metrics)
     * @return a new metric instance
     * @throws MetricInstantiationException if no suitable constructor is found
     */
    public static EvalMetric create(Class<? extends EvalMetric> metricClass,
                                    double threshold,
                                    JudgeModel judgeModel) {
        boolean useDefault = Double.compare(threshold, USE_DEFAULT_THRESHOLD) == 0;

        // Try (JudgeModel, double) if we have both
        if (judgeModel != null && !useDefault) {
            EvalMetric m = tryConstruct(metricClass, new Class<?>[]{JudgeModel.class, double.class},
                    judgeModel, threshold);
            if (m != null) return m;
        }

        // Try (double) if threshold is explicit
        if (!useDefault) {
            EvalMetric m = tryConstruct(metricClass, new Class<?>[]{double.class}, threshold);
            if (m != null) return m;
        }

        // Try (JudgeModel) if available
        if (judgeModel != null) {
            EvalMetric m = tryConstruct(metricClass, new Class<?>[]{JudgeModel.class}, judgeModel);
            if (m != null) return m;
        }

        // Try no-arg
        EvalMetric m = tryConstruct(metricClass, new Class<?>[]{});
        if (m != null) return m;

        throw new MetricInstantiationException(
                "No suitable constructor found for " + metricClass.getName()
                        + ". Expected one of: (JudgeModel, double), (double), (JudgeModel), ()");
    }

    private static EvalMetric tryConstruct(Class<? extends EvalMetric> clazz,
                                           Class<?>[] paramTypes,
                                           Object... args) {
        try {
            Constructor<? extends EvalMetric> ctor = clazz.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (NoSuchMethodException e) {
            LOG.trace("Constructor {} not found on {}", paramTypes, clazz.getSimpleName());
            return null;
        } catch (Exception e) {
            throw new MetricInstantiationException(
                    "Failed to instantiate " + clazz.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Exception thrown when metric instantiation fails.
     */
    public static class MetricInstantiationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public MetricInstantiationException(String message) {
            super(message);
        }

        public MetricInstantiationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
