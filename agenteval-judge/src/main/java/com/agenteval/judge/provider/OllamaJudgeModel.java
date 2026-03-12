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
 * Ollama judge model provider.
 *
 * <p>Sends requests to {@code POST /api/chat} with JSON response format.
 * No API key required.</p>
 */
public final class OllamaJudgeModel extends AbstractHttpJudgeModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String CHAT_PATH = "/api/chat";
    private static final String SYSTEM_PROMPT =
            "You are an evaluation judge. Respond ONLY with a JSON object "
                    + "containing \"score\" (a number between 0.0 and 1.0) "
                    + "and \"reason\" (a brief explanation).";

    public OllamaJudgeModel(JudgeConfig config) {
        super(config);
    }

    OllamaJudgeModel(JudgeConfig config, HttpJudgeClient client) {
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
            body.put("stream", false);
            body.put("format", "json");

            var messages = body.putArray("messages");

            var systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);

            var userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            String url = config.getBaseUrl() + CHAT_PATH;
            return new HttpJudgeRequest(url, Map.of(),
                    MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new JudgeException("Failed to build Ollama request", e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode message = root.path("message");
        if (message.isMissingNode()) {
            throw new JudgeException("No message in Ollama response");
        }
        return message.path("content").asText("");
    }

    @Override
    protected TokenUsage extractTokenUsage(String responseBody) {
        JsonNode root = parseJson(responseBody);
        int promptTokens = root.path("prompt_eval_count").asInt(0);
        int completionTokens = root.path("eval_count").asInt(0);
        if (promptTokens == 0 && completionTokens == 0) {
            return null;
        }
        return new TokenUsage(promptTokens, completionTokens,
                promptTokens + completionTokens);
    }
}
