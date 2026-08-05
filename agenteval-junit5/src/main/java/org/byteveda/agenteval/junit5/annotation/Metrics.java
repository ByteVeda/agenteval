package org.byteveda.agenteval.junit5.annotation;

import org.byteveda.agenteval.junit5.extension.AgentEvalExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeated {@link Metric} annotations.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AgentEvalExtension.class)
public @interface Metrics {
    Metric[] value();
}
