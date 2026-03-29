package org.byteveda.agenteval.junit5.annotation;

import org.byteveda.agenteval.junit5.extension.AgentEvalExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation combining {@code @Test}, {@code @Tag("eval")},
 * and {@code @ExtendWith(AgentEvalExtension.class)}.
 *
 * <pre>{@code
 * @AgentTest
 * @Metric(value = AnswerRelevancyMetric.class, threshold = 0.7)
 * void testRelevancy(AgentTestCase testCase) {
 *     testCase.setActualOutput(agent.ask(testCase.getInput()));
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("eval")
@ExtendWith(AgentEvalExtension.class)
public @interface AgentTest {
}
