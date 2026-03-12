package com.agenteval.judge.http;

import java.util.Map;

/**
 * An HTTP request to a judge LLM endpoint.
 */
public record HttpJudgeRequest(
        String url,
        Map<String, String> headers,
        String body
) {
    public HttpJudgeRequest {
        if (url == null) throw new IllegalArgumentException("url must not be null");
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        if (body == null) throw new IllegalArgumentException("body must not be null");
    }
}
