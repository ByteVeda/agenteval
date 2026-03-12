package com.agenteval.junit5.annotation;

import com.agenteval.junit5.extension.DatasetArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides {@link com.agenteval.core.model.AgentTestCase} arguments from a JSON dataset file.
 *
 * <pre>{@code
 * @ParameterizedTest
 * @DatasetSource("datasets/golden-set.json")
 * @Metric(value = AnswerRelevancyMetric.class, threshold = 0.7)
 * void testWithDataset(AgentTestCase testCase) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(DatasetArgumentsProvider.class)
public @interface DatasetSource {

    /**
     * Classpath resource path to the JSON dataset file.
     */
    String value();
}
