package com.agenteval.judge;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.judge.config.JudgeConfig;
import com.agenteval.judge.provider.AnthropicJudgeModel;
import com.agenteval.judge.provider.OpenAiJudgeModel;

/**
 * Static factory for creating judge model instances.
 *
 * <p>API key resolution order: explicit parameter, environment variable, fail fast.</p>
 *
 * <pre>{@code
 * var judge = JudgeModels.openai("gpt-4o");
 * var judge = JudgeModels.anthropic("claude-sonnet-4-20250514");
 * var judge = JudgeModels.openai(JudgeConfig.builder()
 *     .apiKey("sk-...")
 *     .model("gpt-4o")
 *     .baseUrl("https://api.openai.com")
 *     .build());
 * }</pre>
 */
public final class JudgeModels {

    private static final String OPENAI_API_KEY_ENV = "OPENAI_API_KEY";
    private static final String ANTHROPIC_API_KEY_ENV = "ANTHROPIC_API_KEY";
    private static final String OPENAI_BASE_URL = "https://api.openai.com";
    private static final String ANTHROPIC_BASE_URL = "https://api.anthropic.com";

    private JudgeModels() {}

    /**
     * Creates an OpenAI judge model using the given model ID.
     * API key is resolved from the {@code OPENAI_API_KEY} environment variable.
     */
    public static JudgeModel openai(String model) {
        return openai(JudgeConfig.builder()
                .apiKey(resolveApiKey(OPENAI_API_KEY_ENV, "OpenAI"))
                .model(model)
                .baseUrl(OPENAI_BASE_URL)
                .build());
    }

    /**
     * Creates an OpenAI judge model with full configuration.
     */
    public static JudgeModel openai(JudgeConfig config) {
        return new OpenAiJudgeModel(config);
    }

    /**
     * Creates an Anthropic judge model using the given model ID.
     * API key is resolved from the {@code ANTHROPIC_API_KEY} environment variable.
     */
    public static JudgeModel anthropic(String model) {
        return anthropic(JudgeConfig.builder()
                .apiKey(resolveApiKey(ANTHROPIC_API_KEY_ENV, "Anthropic"))
                .model(model)
                .baseUrl(ANTHROPIC_BASE_URL)
                .build());
    }

    /**
     * Creates an Anthropic judge model with full configuration.
     */
    public static JudgeModel anthropic(JudgeConfig config) {
        return new AnthropicJudgeModel(config);
    }

    private static String resolveApiKey(String envVar, String providerName) {
        String key = System.getenv(envVar);
        if (key == null || key.isBlank()) {
            throw new JudgeException(
                    providerName + " API key not found. Set the " + envVar
                            + " environment variable or provide it via JudgeConfig.builder().apiKey()");
        }
        return key;
    }
}
