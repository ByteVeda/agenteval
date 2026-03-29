package org.byteveda.agenteval.embeddings.provider;

import org.byteveda.agenteval.core.embedding.EmbeddingModel;
import org.byteveda.agenteval.embeddings.EmbeddingException;
import org.byteveda.agenteval.embeddings.config.EmbeddingConfig;
import org.byteveda.agenteval.embeddings.http.HttpEmbeddingClient;
import org.byteveda.agenteval.embeddings.http.HttpEmbeddingRequest;
import org.byteveda.agenteval.embeddings.http.HttpEmbeddingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ollama embedding model provider.
 *
 * <p>Sends requests to {@code POST /api/embeddings}. No API key required.</p>
 */
public final class OllamaEmbeddingModel implements EmbeddingModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String EMBEDDINGS_PATH = "/api/embeddings";

    private final EmbeddingConfig config;
    private final HttpEmbeddingClient client;

    public OllamaEmbeddingModel(EmbeddingConfig config) {
        this(config, new HttpEmbeddingClient(config));
    }

    OllamaEmbeddingModel(EmbeddingConfig config, HttpEmbeddingClient client) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    @Override
    public List<Double> embed(String text) {
        Objects.requireNonNull(text, "text must not be null");
        try {
            var body = MAPPER.createObjectNode();
            body.put("model", config.getModel());
            body.put("prompt", text);

            String url = config.getBaseUrl() + EMBEDDINGS_PATH;
            var request = new HttpEmbeddingRequest(url, Map.of(),
                    MAPPER.writeValueAsString(body));

            HttpEmbeddingResponse response = client.send(request);
            if (!response.isSuccess()) {
                throw new EmbeddingException(
                        "Ollama embedding request failed with status " + response.statusCode());
            }

            return parseEmbedding(response.body());
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to generate Ollama embedding", e);
        }
    }

    @Override
    public String modelId() {
        return config.getModel();
    }

    private List<Double> parseEmbedding(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode embedding = root.path("embedding");
            if (embedding.isMissingNode() || embedding.isEmpty()) {
                throw new EmbeddingException("No embedding in Ollama response");
            }
            List<Double> result = new ArrayList<>(embedding.size());
            for (JsonNode val : embedding) {
                result.add(val.asDouble());
            }
            return result;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to parse Ollama embedding response", e);
        }
    }
}
