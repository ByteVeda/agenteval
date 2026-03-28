package com.agenteval.junit5.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a golden dataset into a parameterized test parameter.
 *
 * <p>Applied to a method parameter of type {@code EvalDataset} or
 * {@code List<AgentTestCase>} to automatically load and inject a golden
 * dataset from the specified path.</p>
 *
 * <pre>{@code
 * @AgentTest
 * void testWithGoldenSet(@GoldenSet("src/test/resources/golden.json") EvalDataset dataset) {
 *     for (var testCase : dataset.testCases()) {
 *         testCase.setActualOutput(agent.ask(testCase.getInput()));
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface GoldenSet {

    /**
     * Path to the golden dataset file (JSON, JSONL, CSV, or YAML).
     * Resolved relative to the project root or classpath.
     */
    String value();
}
