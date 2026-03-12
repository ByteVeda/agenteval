package com.agenteval.judge.http;

/**
 * An HTTP response from a judge LLM endpoint.
 */
public record HttpJudgeResponse(
        int statusCode,
        String body,
        String retryAfterHeader
) {
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public boolean isRateLimited() {
        return statusCode == 429;
    }

    public boolean isServerError() {
        return statusCode >= 500;
    }

    public boolean isRetryable() {
        return isRateLimited() || isServerError();
    }
}
