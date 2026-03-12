package com.agenteval.embeddings.provider;

import com.agenteval.core.embedding.EmbeddingModel;
import com.agenteval.embeddings.EmbeddingException;
import com.agenteval.embeddings.config.EmbeddingConfig;
import com.agenteval.embeddings.http.HttpEmbeddingClient;
import com.agenteval.embeddings.http.HttpEmbeddingRequest;
import com.agenteval.embeddings.http.HttpEmbeddingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenAI embedding model provider.
 *
 * <p>Sends requests to {@code POST /v1/embeddings}.</p>
 */
public final class OpenAiEmbeddingModel implements EmbeddingModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    private final EmbeddingConfig config;
    private final HttpEmbeddingClient client;

    public OpenAiEmbeddingModel(EmbeddingConfig config) {
        this(config, new HttpEmbeddingClient(config));
    }

    OpenAiEmbeddingModel(EmbeddingConfig config, HttpEmbeddingClient client) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public List<Double> embed(String text) {
        Objects.requireNonNull(text, "text must not be null");
        try {
            var body = MAPPER.createObjectNode();
            body.put("model", config.getModel());
            body.put("input", text);

            String url = config.getBaseUrl() + EMBEDDINGS_PATH;
            var request = new HttpEmbeddingRequest(
                    url,
                    Map.of("Authorization", "Bearer " + config.getApiKey()),
                    MAPPER.writeValueAsString(body));

            HttpEmbeddingResponse response = client.send(request);
            if (!response.isSuccess()) {
                throw new EmbeddingException(
                        "OpenAI embedding request failed with status " + response.statusCode());
            }

            return parseEmbedding(response.body());
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to generate OpenAI embedding", e);
        }
    }

    @Override
    public String modelId() {
        return config.getModel();
    }

    private List<Double> parseEmbedding(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode data = root.path("data");
            if (data.isEmpty()) {
                throw new EmbeddingException("No data in OpenAI embedding response");
            }
            JsonNode embedding = data.get(0).path("embedding");
            List<Double> result = new ArrayList<>(embedding.size());
            for (JsonNode val : embedding) {
                result.add(val.asDouble());
            }
            return result;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to parse OpenAI embedding response", e);
        }
    }
}
