package com.agenteval.embeddings.provider;

import com.agenteval.core.embedding.EmbeddingModel;
import com.agenteval.embeddings.EmbeddingException;
import com.agenteval.embeddings.config.CustomEmbeddingConfig;
import com.agenteval.embeddings.config.EmbeddingConfig;
import com.agenteval.embeddings.http.HttpEmbeddingClient;
import com.agenteval.embeddings.http.HttpEmbeddingRequest;
import com.agenteval.embeddings.http.HttpEmbeddingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Embedding model provider for custom HTTP endpoints.
 *
 * <p>Allows configuring arbitrary request templates and response JSON path extraction,
 * enabling integration with any embedding API.</p>
 */
public final class CustomHttpEmbeddingModel implements EmbeddingModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern INPUT_PLACEHOLDER = Pattern.compile("\\{\\{input}}");

    private final EmbeddingConfig config;
    private final CustomEmbeddingConfig customConfig;
    private final HttpEmbeddingClient client;

    public CustomHttpEmbeddingModel(EmbeddingConfig config, CustomEmbeddingConfig customConfig) {
        this(config, customConfig, new HttpEmbeddingClient(config));
    }

    CustomHttpEmbeddingModel(EmbeddingConfig config, CustomEmbeddingConfig customConfig,
                             HttpEmbeddingClient client) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.customConfig = Objects.requireNonNull(customConfig,
                "customConfig must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public List<Double> embed(String text) {
        Objects.requireNonNull(text, "text must not be null");
        try {
            String body = renderTemplate(customConfig.getRequestTemplate(), text);
            String url = config.getBaseUrl();

            Map<String, String> headers = new HashMap<>();
            if (customConfig.getAuthHeader() != null) {
                headers.put("Authorization", customConfig.getAuthHeader());
            }

            var request = new HttpEmbeddingRequest(url, headers, body);
            HttpEmbeddingResponse response = client.send(request);

            if (!response.isSuccess()) {
                throw new EmbeddingException(
                        "Custom embedding request failed with status " + response.statusCode());
            }

            return parseEmbedding(response.body(), customConfig.getEmbeddingJsonPath());
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to generate custom embedding", e);
        }
    }

    @Override
    public String modelId() {
        return config.getModel();
    }

    static String renderTemplate(String template, String input) {
        String escaped = Matcher.quoteReplacement(
                input.replace("\\", "\\\\").replace("\"", "\\\""));
        return INPUT_PLACEHOLDER.matcher(template).replaceAll(escaped);
    }

    static List<Double> parseEmbedding(String responseBody, String jsonPath) {
        try {
            JsonNode node = MAPPER.readTree(responseBody);
            String[] segments = jsonPath.split("\\.");

            for (String segment : segments) {
                if (node == null || node.isMissingNode()) {
                    throw new EmbeddingException(
                            "JSON path '" + jsonPath + "' not found in response");
                }
                try {
                    int index = Integer.parseInt(segment);
                    node = node.get(index);
                } catch (NumberFormatException e) {
                    node = node.get(segment);
                }
            }

            if (node == null || !node.isArray()) {
                throw new EmbeddingException(
                        "Expected array at JSON path '" + jsonPath + "', got: "
                                + (node == null ? "null" : node.getNodeType()));
            }

            List<Double> result = new ArrayList<>(node.size());
            for (JsonNode val : node) {
                result.add(val.asDouble());
            }
            return result;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to parse custom embedding response", e);
        }
    }
}
