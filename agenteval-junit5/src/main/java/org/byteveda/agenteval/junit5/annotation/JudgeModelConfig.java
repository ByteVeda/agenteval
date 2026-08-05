package org.byteveda.agenteval.junit5.annotation;

import org.byteveda.agenteval.junit5.extension.AgentEvalExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the judge LLM for a specific test class or method.
 *
 * <p>When placed on a test class, all {@code @AgentTest} methods in that class
 * use the specified judge. When placed on a method, it overrides the class-level
 * configuration for that method only.</p>
 *
 * <pre>{@code
 * @JudgeModelConfig(provider = "openai", model = "gpt-4o-mini")
 * class FastEvalTests {
 *
 *     @AgentTest
 *     @JudgeModelConfig(provider = "anthropic", model = "claude-sonnet-4-20250514")
 *     void highQualityTest(AgentTestCase testCase) { ... }
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AgentEvalExtension.class)
public @interface JudgeModelConfig {

    /**
     * The judge provider name (e.g., "openai", "anthropic", "ollama").
     */
    String provider();

    /**
     * The model identifier (e.g., "gpt-4o", "claude-sonnet-4-20250514").
     */
    String model();

    /**
     * API key for the judge provider. Defaults to empty string,
     * which means the key is read from the standard environment variable.
     */
    String apiKey() default "";
}
