package com.agenteval.judge;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.judge.config.JudgeConfig;
import com.agenteval.judge.multi.MultiModelJudge;
import com.agenteval.judge.provider.AnthropicJudgeModel;
import com.agenteval.judge.provider.AzureOpenAiJudgeModel;
import com.agenteval.judge.provider.BedrockJudgeModel;
import com.agenteval.judge.provider.CustomHttpJudgeModel;
import com.agenteval.judge.provider.GoogleJudgeModel;
import com.agenteval.judge.provider.OllamaJudgeModel;
import com.agenteval.judge.provider.OpenAiJudgeModel;

/**
 * Static factory for creating judge model instances.
 *
 * <p>API key resolution order: explicit parameter, environment variable, fail fast.</p>
 *
 * <pre>{@code
 * var judge = JudgeModels.openai("gpt-4o");
 * var judge = JudgeModels.anthropic("claude-sonnet-4-20250514");
 * var judge = JudgeModels.google("gemini-1.5-pro");
 * var judge = JudgeModels.azure(JudgeConfig.builder()
 *     .apiKey("...").model("my-deployment").baseUrl("https://myresource.openai.azure.com").build());
 * var judge = JudgeModels.bedrock("anthropic.claude-3-sonnet-20240229-v1:0");
 * var judge = JudgeModels.custom(JudgeConfig.builder()
 *     .model("my-model").baseUrl("http://localhost:8000").build());
 * var judge = JudgeModels.ollama("llama3");
 * }</pre>
 */
public final class JudgeModels {

    private static final String OPENAI_API_KEY_ENV = "OPENAI_API_KEY";
    private static final String ANTHROPIC_API_KEY_ENV = "ANTHROPIC_API_KEY";
    private static final String GOOGLE_API_KEY_ENV = "GOOGLE_API_KEY";
    private static final String OPENAI_BASE_URL = "https://api.openai.com";
    private static final String ANTHROPIC_BASE_URL = "https://api.anthropic.com";
    private static final String GOOGLE_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String BEDROCK_BASE_URL = "https://bedrock-runtime.us-east-1.amazonaws.com";
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";

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

    /**
     * Creates a Google Gemini judge model using the given model ID.
     * API key is resolved from the {@code GOOGLE_API_KEY} environment variable.
     */
    public static JudgeModel google(String model) {
        return google(JudgeConfig.builder()
                .apiKey(resolveApiKey(GOOGLE_API_KEY_ENV, "Google"))
                .model(model)
                .baseUrl(GOOGLE_BASE_URL)
                .build());
    }

    /**
     * Creates a Google Gemini judge model with full configuration.
     */
    public static JudgeModel google(JudgeConfig config) {
        return new GoogleJudgeModel(config);
    }

    /**
     * Creates an Azure OpenAI judge model using the given deployment name.
     * API key is resolved from the {@code AZURE_OPENAI_API_KEY} environment variable.
     * The base URL must point to your Azure OpenAI resource
     * (e.g., {@code https://myresource.openai.azure.com}).
     */
    public static JudgeModel azure(JudgeConfig config) {
        return new AzureOpenAiJudgeModel(config);
    }

    /**
     * Creates an Azure OpenAI judge model with a specific API version.
     */
    public static JudgeModel azure(JudgeConfig config, String apiVersion) {
        return new AzureOpenAiJudgeModel(config, apiVersion);
    }

    /**
     * Creates an Amazon Bedrock judge model using the given model ID.
     * AWS credentials are resolved from {@code AWS_ACCESS_KEY_ID} and
     * {@code AWS_SECRET_ACCESS_KEY} environment variables.
     *
     * @param model the Bedrock model ID (e.g., {@code anthropic.claude-3-sonnet-20240229-v1:0})
     */
    public static JudgeModel bedrock(String model) {
        return bedrock(JudgeConfig.builder()
                .model(model)
                .baseUrl(BEDROCK_BASE_URL)
                .build());
    }

    /**
     * Creates an Amazon Bedrock judge model with full configuration.
     * The base URL determines the AWS region
     * (e.g., {@code https://bedrock-runtime.eu-west-1.amazonaws.com}).
     */
    public static JudgeModel bedrock(JudgeConfig config) {
        return new BedrockJudgeModel(config);
    }

    /**
     * Creates a custom HTTP judge model for any OpenAI-compatible endpoint.
     * No API key required — if one is set in the config, it will be sent
     * as a {@code Bearer} token.
     *
     * <p>Use this for vLLM, LiteLLM, LocalAI, or any OpenAI-compatible server.</p>
     */
    public static JudgeModel custom(JudgeConfig config) {
        return new CustomHttpJudgeModel(config);
    }

    /**
     * Creates an Ollama judge model using the given model ID.
     * Defaults to {@code localhost:11434}. No API key required.
     */
    public static JudgeModel ollama(String model) {
        return ollama(JudgeConfig.builder()
                .model(model)
                .baseUrl(OLLAMA_BASE_URL)
                .build());
    }

    /**
     * Creates an Ollama judge model with full configuration.
     */
    public static JudgeModel ollama(JudgeConfig config) {
        return new OllamaJudgeModel(config);
    }

    /**
     * Creates a multi-model judge builder for combining multiple judge models.
     *
     * <pre>{@code
     * var judge = JudgeModels.multi()
     *     .add(JudgeModels.openai("gpt-4o"))
     *     .add(JudgeModels.anthropic("claude-sonnet-4-20250514"), 2.0)
     *     .strategy(ConsensusStrategy.WEIGHTED_AVERAGE)
     *     .build();
     * }</pre>
     */
    public static MultiModelJudge.Builder multi() {
        return MultiModelJudge.builder();
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
