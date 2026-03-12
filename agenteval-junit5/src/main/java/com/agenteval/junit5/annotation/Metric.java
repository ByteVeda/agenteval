package com.agenteval.junit5.annotation;

import com.agenteval.core.metric.EvalMetric;
import com.agenteval.junit5.extension.AgentEvalExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an evaluation metric to apply to an {@code @AgentTest} method.
 *
 * <p>Repeatable — multiple metrics can be applied to a single test method.</p>
 *
 * <pre>{@code
 * @AgentTest
 * @Metric(value = AnswerRelevancyMetric.class, threshold = 0.7)
 * @Metric(value = FaithfulnessMetric.class, threshold = 0.8)
 * void testQuality(AgentTestCase testCase) { ... }
 * }</pre>
 *
 * <p>A threshold of {@code -1.0} means "use the metric's default threshold".</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Metrics.class)
@ExtendWith(AgentEvalExtension.class)
public @interface Metric {

    /**
     * The metric class to instantiate.
     */
    Class<? extends EvalMetric> value();

    /**
     * Score threshold (0.0–1.0). Use {@code -1.0} to defer to the metric's default.
     */
    double threshold() default -1.0;
}
