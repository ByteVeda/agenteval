package org.byteveda.agenteval.junit5.extension;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Resolves a {@link JudgeModel} from provider name, model ID, and API key.
 *
 * <p>Attempts to load the appropriate judge model implementation from
 * the {@code agenteval-judge} module via reflection, falling back to a
 * no-op model if the provider class is not on the classpath.</p>
 */
final class JudgeModelResolver {

    private static final Logger LOG = LoggerFactory.getLogger(JudgeModelResolver.class);

    private static final String JUDGE_PACKAGE = "org.byteveda.agenteval.judge.provider.";

    private JudgeModelResolver() {}

    /**
     * Resolves a judge model for the given provider and model ID.
     *
     * @param provider the provider name (e.g., "openai", "anthropic")
     * @param model the model identifier
     * @param apiKey the API key (may be null)
     * @return a configured JudgeModel instance
     */
    static JudgeModel resolve(String provider, String model, String apiKey) {
        String className = resolveClassName(provider);
        try {
            Class<?> clazz = Class.forName(className);
            return instantiate(clazz, model, apiKey);
        } catch (ClassNotFoundException e) {
            LOG.warn("Judge provider class '{}' not found on classpath. "
                    + "Add agenteval-judge dependency.", className);
            throw new MetricFactory.MetricInstantiationException(
                    "Judge provider '" + provider + "' not found. "
                            + "Ensure agenteval-judge is on the classpath.");
        }
    }

    private static String resolveClassName(String provider) {
        return switch (provider.toLowerCase()) {
            case "openai" -> JUDGE_PACKAGE + "OpenAiJudgeModel";
            case "anthropic" -> JUDGE_PACKAGE + "AnthropicJudgeModel";
            case "google" -> JUDGE_PACKAGE + "GoogleJudgeModel";
            case "ollama" -> JUDGE_PACKAGE + "OllamaJudgeModel";
            case "azure", "azure_openai" -> JUDGE_PACKAGE + "AzureOpenAiJudgeModel";
            case "bedrock" -> JUDGE_PACKAGE + "BedrockJudgeModel";
            default -> JUDGE_PACKAGE + "CustomHttpJudgeModel";
        };
    }

    private static JudgeModel instantiate(Class<?> clazz, String model, String apiKey) {
        // Try (String modelId, String apiKey)
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor(String.class, String.class);
            return (JudgeModel) ctor.newInstance(model, apiKey);
        } catch (NoSuchMethodException ignored) {
            // fall through
        } catch (Exception e) {
            throw new MetricFactory.MetricInstantiationException(
                    "Failed to instantiate judge: " + e.getMessage(), e);
        }

        // Try JudgeConfig-based constructor via builder if available
        try {
            Class<?> configClass = Class.forName("org.byteveda.agenteval.judge.config.JudgeConfig");
            Object configBuilder = configClass.getMethod("builder").invoke(null);
            Class<?> builderClass = configBuilder.getClass();
            builderClass.getMethod("modelId", String.class).invoke(configBuilder, model);
            if (apiKey != null) {
                builderClass.getMethod("apiKey", String.class).invoke(configBuilder, apiKey);
            }
            Object judgeConfig = builderClass.getMethod("build").invoke(configBuilder);
            Constructor<?> ctor = clazz.getDeclaredConstructor(configClass);
            return (JudgeModel) ctor.newInstance(judgeConfig);
        } catch (Exception e) {
            throw new MetricFactory.MetricInstantiationException(
                    "Failed to instantiate judge model for provider "
                            + clazz.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
