package org.byteveda.agenteval.embeddings.http;

import java.util.Map;

/**
 * An HTTP request to an embedding model endpoint.
 */
public record HttpEmbeddingRequest(
        String url,
        Map<String, String> headers,
        String body
) {
    public HttpEmbeddingRequest {
        if (url == null) throw new IllegalArgumentException("url must not be null");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        if (body == null) throw new IllegalArgumentException("body must not be null");
    }
}
