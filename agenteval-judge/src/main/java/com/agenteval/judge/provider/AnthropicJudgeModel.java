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
 * Anthropic judge model provider.
 *
 * <p>Sends requests to {@code POST /v1/messages} with
 * {@code x-api-key} header authentication.</p>
 */
public final class AnthropicJudgeModel extends AbstractHttpJudgeModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final String MESSAGES_PATH = "/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final String SYSTEM_PROMPT =
            "You are an evaluation judge. Respond ONLY with a JSON object "
                    + "containing \"score\" (a number between 0.0 and 1.0) "
                    + "and \"reason\" (a brief explanation).";

    public AnthropicJudgeModel(JudgeConfig config) {
        super(config);
    }

    AnthropicJudgeModel(JudgeConfig config, HttpJudgeClient client) {
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
            body.put("max_tokens", 1024);
            body.put("temperature", config.getTemperature());
            body.put("system", SYSTEM_PROMPT);

            var messages = body.putArray("messages");
            var userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            String url = config.getBaseUrl() + MESSAGES_PATH;
            return new HttpJudgeRequest(
                    url,
                    Map.of(
                            "x-api-key", config.getApiKey(),
                            "anthropic-version", API_VERSION),
                    MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new JudgeException("Failed to build Anthropic request", e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode content = root.path("content");
        if (content.isEmpty()) {
            throw new JudgeException("No content in Anthropic response");
        }
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                return block.path("text").asText("");
            }
        }
        throw new JudgeException("No text block in Anthropic response content");
    }

    @Override
    protected TokenUsage extractTokenUsage(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode usage = root.path("usage");
        if (usage.isMissingNode()) {
            return null;
        }
        int input = usage.path("input_tokens").asInt(0);
        int output = usage.path("output_tokens").asInt(0);
        return new TokenUsage(input, output, input + output);
    }
}
