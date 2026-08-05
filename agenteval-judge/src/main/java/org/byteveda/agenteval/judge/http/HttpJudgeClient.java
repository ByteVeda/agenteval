package org.byteveda.agenteval.judge.http;

import org.byteveda.agenteval.judge.JudgeException;
import org.byteveda.agenteval.judge.JudgeRateLimitException;
import org.byteveda.agenteval.judge.JudgeTimeoutException;
import org.byteveda.agenteval.judge.config.JudgeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HTTP client with exponential backoff retry for judge LLM requests.
 *
 * <p>Retries on 429 (rate limit) and 5xx (server error) responses.
 * Respects {@code Retry-After} header when present.</p>
 */
public class HttpJudgeClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpJudgeClient.class);
    private static final Duration BASE_DELAY = Duration.ofMillis(500);
    private static final double JITTER_FACTOR = 0.5;

    private final HttpClient httpClient;
    private final int maxRetries;
    private final Duration timeout;

    public HttpJudgeClient(JudgeConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(config.getTimeout())
                .build());
    }

    HttpJudgeClient(JudgeConfig config, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.maxRetries = config.getMaxRetries();
        this.timeout = config.getTimeout();
    }

    /**
     * Sends a request with retry on transient failures.
     */
    public HttpJudgeResponse send(HttpJudgeRequest request) {
        int attempt = 0;
        while (true) {
            HttpJudgeResponse response = doSend(request);
            if (response.isSuccess() || !response.isRetryable() || attempt >= maxRetries) {
                if (!response.isSuccess() && response.isRateLimited()) {
                    Duration retryAfter = parseRetryAfter(response.retryAfterHeader());
                    throw new JudgeRateLimitException(
                            "Rate limited after " + (attempt + 1) + " attempt(s): "
                                    + response.statusCode(),
                            retryAfter);
                }
                if (!response.isSuccess()) {
                    throw new JudgeException(
                            "Judge request failed with status " + response.statusCode()
                                    + ": " + truncate(response.body(), 200));
                }
                return response;
            }
            Duration delay = calculateDelay(attempt, response.retryAfterHeader());
            LOG.debug("Retrying judge request (attempt {}/{}), waiting {}ms",
                    attempt + 1, maxRetries, delay.toMillis());
            sleep(delay);
            attempt++;
        }
    }

    private HttpJudgeResponse doSend(HttpJudgeRequest request) {
        try {
            var builder = HttpRequest.newBuilder()
                    .uri(URI.create(request.url()))
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(request.body()));
            request.headers().forEach(builder::header);
            builder.header("Content-Type", "application/json");

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            String retryAfter = response.headers()
                    .firstValue("retry-after")
                    .orElse(null);

            return new HttpJudgeResponse(
                    response.statusCode(),
                    response.body(),
                    retryAfter);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new JudgeTimeoutException(
                    "Judge request timed out after " + timeout, timeout, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JudgeException("Judge request interrupted", e);
        } catch (java.io.IOException e) {
            throw new JudgeException("Judge request failed: " + e.getMessage(), e);
        }
    }

    Duration calculateDelay(int attempt, String retryAfterHeader) {
        Duration retryAfter = parseRetryAfter(retryAfterHeader);
        if (retryAfter != null) {
            return retryAfter;
        }
        long baseMs = BASE_DELAY.toMillis() * (1L << attempt);
        long jitterMs = (long) (baseMs * JITTER_FACTOR
                * ThreadLocalRandom.current().nextDouble());
        return Duration.ofMillis(baseMs + jitterMs);
    }

    private Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(header.trim());
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JudgeException("Retry sleep interrupted", e);
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
