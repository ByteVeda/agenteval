package org.byteveda.agenteval.junit5.annotation;

import org.byteveda.agenteval.junit5.extension.AgentEvalExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Sets the maximum time allowed for metric evaluation to complete.
 *
 * <p>If the evaluation (including LLM judge calls) exceeds the specified
 * duration, the test fails with a timeout error.</p>
 *
 * <pre>{@code
 * @AgentTest
 * @EvalTimeout(value = 30, unit = TimeUnit.SECONDS)
 * @Metric(value = FaithfulnessMetric.class, threshold = 0.8)
 * void testWithTimeout(AgentTestCase testCase) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AgentEvalExtension.class)
public @interface EvalTimeout {

    /**
     * The timeout duration.
     */
    long value();

    /**
     * The time unit for the timeout. Defaults to {@link TimeUnit#SECONDS}.
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
