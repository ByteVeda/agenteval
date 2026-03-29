package org.byteveda.agenteval.embeddings.http;

/**
 * An HTTP response from an embedding model endpoint.
 */
public record HttpEmbeddingResponse(
        int statusCode,
        String body
) {
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
