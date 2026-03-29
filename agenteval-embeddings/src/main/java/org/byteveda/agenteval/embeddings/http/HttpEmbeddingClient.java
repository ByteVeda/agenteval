package org.byteveda.agenteval.embeddings.http;

import org.byteveda.agenteval.embeddings.EmbeddingException;
import org.byteveda.agenteval.embeddings.config.EmbeddingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * HTTP client for embedding model requests.
 */
public class HttpEmbeddingClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEmbeddingClient.class);

    private final HttpClient httpClient;
    private final EmbeddingConfig config;

    public HttpEmbeddingClient(EmbeddingConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(config.getTimeout())
                .build());
    }

    HttpEmbeddingClient(EmbeddingConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    /**
     * Sends an embedding request and returns the response.
     */
    public HttpEmbeddingResponse send(HttpEmbeddingRequest request) {
        LOG.debug("Sending embedding request to {}", request.url());
        try {
            var builder = HttpRequest.newBuilder()
                    .uri(URI.create(request.url()))
                    .timeout(config.getTimeout())
                    .POST(HttpRequest.BodyPublishers.ofString(request.body()));
            request.headers().forEach(builder::header);
            builder.header("Content-Type", "application/json");

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return new HttpEmbeddingResponse(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Embedding request interrupted", e);
        } catch (java.io.IOException e) {
            throw new EmbeddingException("Embedding request failed: " + e.getMessage(), e);
        }
    }
}
