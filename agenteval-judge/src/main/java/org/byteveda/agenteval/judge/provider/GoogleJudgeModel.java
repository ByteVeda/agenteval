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
 * Google Gemini judge model provider.
 *
 * <p>Sends requests to the Gemini {@code generateContent} API with
 * JSON response format via {@code responseMimeType}.</p>
 */
public final class GoogleJudgeModel extends AbstractHttpJudgeModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GENERATE_PATH = "/v1beta/models/%s:generateContent";
    private static final String SYSTEM_PROMPT =
            "You are an evaluation judge. Respond ONLY with a JSON object "
                    + "containing \"score\" (a number between 0.0 and 1.0) "
                    + "and \"reason\" (a brief explanation).";

    public GoogleJudgeModel(JudgeConfig config) {
        super(config);
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new JudgeException("Google requires a non-null API key");
        }
    }

    GoogleJudgeModel(JudgeConfig config, HttpJudgeClient client) {
        super(config, client);
    }

    static String defaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    protected HttpJudgeRequest buildRequest(String prompt) {
        try {
            var body = MAPPER.createObjectNode();

            var systemInstruction = MAPPER.createObjectNode();
            var systemParts = systemInstruction.putArray("parts");
            systemParts.addObject().put("text", SYSTEM_PROMPT);
            body.set("systemInstruction", systemInstruction);

            var contents = body.putArray("contents");
            var userContent = contents.addObject();
            userContent.put("role", "user");
            var parts = userContent.putArray("parts");
            parts.addObject().put("text", prompt);

            var generationConfig = MAPPER.createObjectNode();
            generationConfig.put("temperature", config.getTemperature());
            generationConfig.put("responseMimeType", "application/json");
            body.set("generationConfig", generationConfig);

            String url = config.getBaseUrl()
                    + String.format(GENERATE_PATH, config.getModel());
            return new HttpJudgeRequest(
                    url,
                    Map.of("x-goog-api-key", config.getApiKey()),
                    MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new JudgeException("Failed to build Google request", e);
        }
    }

    @Override
    protected String extractContent(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            throw new JudgeException("No candidates in Google response");
        }
        return candidates.get(0)
                .path("content").path("parts").path(0)
                .path("text").asText("");
    }

    @Override
    protected TokenUsage extractTokenUsage(String responseBody) {
        JsonNode root = parseJson(responseBody);
        JsonNode usage = root.path("usageMetadata");
        if (usage.isMissingNode()) {
            return null;
        }
        int promptTokens = usage.path("promptTokenCount").asInt(0);
        int candidateTokens = usage.path("candidatesTokenCount").asInt(0);
        int totalTokens = usage.path("totalTokenCount").asInt(0);
        if (totalTokens == 0) {
            totalTokens = promptTokens + candidateTokens;
        }
        return new TokenUsage(promptTokens, candidateTokens, totalTokens);
    }
}
