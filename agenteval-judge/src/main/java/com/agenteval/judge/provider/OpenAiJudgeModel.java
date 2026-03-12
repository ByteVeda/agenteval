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
 * OpenAI-compatible judge model provider.
 *
 * <p>Sends requests to {@code POST /v1/chat/completions} with
 * {@code response_format: {"type": "json_object"}} for structured output.</p>
 */
public final class OpenAiJudgeModel extends AbstractHttpJudgeModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final String COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String SYSTEM_PROMPT =
            "You are an evaluation judge. Respond ONLY with a JSON object "
                    + "containing \"score\" (a number between 0.0 and 1.0) "
                    + "and \"reason\" (a brief explanation).";

    public OpenAiJudgeModel(JudgeConfig config) {
        super(config);
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new JudgeException("OpenAI requires a non-null API key");
        }
    }

    OpenAiJudgeModel(JudgeConfig config, HttpJudgeClient client) {
        super(config, client);
    }

    static String defaultBaseUrl() {
        return DEFAULT_BASE_URL;
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
            return new HttpJudgeRequest(
                    url,
                    Map.of("Authorization", "Bearer " + config.getApiKey()),
                    MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new JudgeException("Failed to build OpenAI request", e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            throw new JudgeException("No choices in OpenAI response");
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
