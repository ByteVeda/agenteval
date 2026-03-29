package org.byteveda.agenteval.judge.provider;

import org.byteveda.agenteval.core.model.TokenUsage;
import org.byteveda.agenteval.judge.JudgeException;
import org.byteveda.agenteval.judge.config.JudgeConfig;
import org.byteveda.agenteval.judge.http.HttpJudgeClient;
import org.byteveda.agenteval.judge.http.HttpJudgeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Azure OpenAI judge model provider.
 *
 * <p>Sends requests to Azure's OpenAI-compatible endpoint at
 * {@code {baseUrl}/openai/deployments/{model}/chat/completions?api-version=...}
 * with {@code api-key} header authentication.</p>
 *
 * <p>The {@link JudgeConfig#getModel()} value is used as the deployment name.
 * The API version defaults to {@code 2024-02-01} but can be customized
 * by appending it to the base URL.</p>
 */
public final class AzureOpenAiJudgeModel extends AbstractHttpJudgeModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_API_VERSION = "2024-02-01";
    private static final String COMPLETIONS_PATH =
            "/openai/deployments/%s/chat/completions?api-version=%s";
    private static final String SYSTEM_PROMPT =
            "You are an evaluation judge. Respond ONLY with a JSON object "
                    + "containing \"score\" (a number between 0.0 and 1.0) "
                    + "and \"reason\" (a brief explanation).";

    private final String apiVersion;

    public AzureOpenAiJudgeModel(JudgeConfig config) {
        this(config, DEFAULT_API_VERSION);
    }

    public AzureOpenAiJudgeModel(JudgeConfig config, String apiVersion) {
        super(config);
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new JudgeException("Azure OpenAI requires a non-null API key");
        }
        this.apiVersion = apiVersion != null ? apiVersion : DEFAULT_API_VERSION;
    }

    AzureOpenAiJudgeModel(JudgeConfig config, String apiVersion, HttpJudgeClient client) {
        super(config, client);
        this.apiVersion = apiVersion != null ? apiVersion : DEFAULT_API_VERSION;
    }

    String getApiVersion() {
        return apiVersion;
    }

    @Override
    protected HttpJudgeRequest buildRequest(String prompt) {
        try {
            var body = MAPPER.createObjectNode();
            body.put("temperature", config.getTemperature());

            var responseFormat = MAPPER.createObjectNode();
            responseFormat.put("type", "json_object");
            body.set("response_format", responseFormat);

            var messages = body.putArray("messages");

            var systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);

            var userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            String url = config.getBaseUrl()
                    + String.format(COMPLETIONS_PATH, config.getModel(), apiVersion);
            return new HttpJudgeRequest(
                    url,
                    Map.of("api-key", config.getApiKey()),
                    MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new JudgeException("Failed to build Azure OpenAI request", e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            throw new JudgeException("No choices in Azure OpenAI response");
        }
        return choices.get(0).path("message").path("content").asText("");
    }

    @Override
    protected TokenUsage extractTokenUsage(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode usage = root.path("usage");
        if (usage.isMissingNode()) {
            return null;
        }
        return new TokenUsage(
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0),
                usage.path("total_tokens").asInt(0));
    }
}
