package com.agenteval.judge.provider;

import com.agenteval.core.model.TokenUsage;
import com.agenteval.judge.JudgeException;
import com.agenteval.judge.config.JudgeConfig;
import com.agenteval.judge.http.HttpJudgeClient;
import com.agenteval.judge.http.HttpJudgeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Custom HTTP judge model provider for any OpenAI-compatible endpoint.
 *
 * <p>Points to any self-hosted or third-party API that exposes
 * the OpenAI chat completions format. Supports optional API key
 * authentication via {@code Authorization: Bearer} header.</p>
 *
 * <p>Use this provider for vLLM, LiteLLM, LocalAI, or any other
 * OpenAI-compatible inference server.</p>
 *
 * <pre>{@code
 * var judge = JudgeModels.custom(JudgeConfig.builder()
 *     .model("my-model")
 *     .baseUrl("http://localhost:8000")
 *     .build());
 * }</pre>
 */
public final class CustomHttpJudgeModel extends AbstractHttpJudgeModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String SYSTEM_PROMPT =
            "You are an evaluation judge. Respond ONLY with a JSON object "
                    + "containing \"score\" (a number between 0.0 and 1.0) "
                    + "and \"reason\" (a brief explanation).";

    public CustomHttpJudgeModel(JudgeConfig config) {
        super(config);
    }

    CustomHttpJudgeModel(JudgeConfig config, HttpJudgeClient client) {
        super(config, client);
    }

    @Override
    protected HttpJudgeRequest buildRequest(String prompt) {
        try {
            var body = MAPPER.createObjectNode();
            body.put("model", config.getModel());
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

            String url = config.getBaseUrl() + COMPLETIONS_PATH;
            Map<String, String> headers = buildHeaders();
            return new HttpJudgeRequest(url, headers,
                    MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new JudgeException("Failed to build custom HTTP request", e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            throw new JudgeException("No choices in custom HTTP response");
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

    private Map<String, String> buildHeaders() {
        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            return Map.of("Authorization", "Bearer " + apiKey);
        }
        return Map.of();
    }
}
