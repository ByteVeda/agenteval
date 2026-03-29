package org.byteveda.agenteval.judge.http;

import org.byteveda.agenteval.judge.JudgeException;
import org.byteveda.agenteval.judge.JudgeRateLimitException;
import org.byteveda.agenteval.judge.JudgeTimeoutException;
import org.byteveda.agenteval.judge.config.JudgeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class HttpJudgeClientTest {

    private JudgeConfig config;

    @BeforeEach
    void setUp() {
        config = JudgeConfig.builder()
                .apiKey("test-key")
                .model("test-model")
                .baseUrl("https://test.api.com")
                .maxRetries(2)
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @Test
    void shouldReturnResponseOnSuccess() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"result\": \"ok\"}");
        when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of(), (a, b) -> true));
        doReturn(mockResponse).when(mockHttp).send(any(HttpRequest.class), any());

        var client = new HttpJudgeClient(config, mockHttp);
        var request = new HttpJudgeRequest(
                "https://test.api.com/v1/completions",
                Map.of("Authorization", "Bearer test"),
                "{\"prompt\": \"test\"}");

        HttpJudgeResponse response = client.send(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void shouldThrowOnRateLimitAfterRetries() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(429);
        when(mockResponse.body()).thenReturn("Rate limited");
        when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of("retry-after", java.util.List.of("5")), (a, b) -> true));
        doReturn(mockResponse).when(mockHttp).send(any(HttpRequest.class), any());

        var client = new HttpJudgeClient(config, mockHttp) {
            @Override
            void sleep(Duration duration) {
                // no-op for testing
            }
        };

        var request = new HttpJudgeRequest(
                "https://test.api.com/v1/completions",
                Map.of(), "{\"prompt\": \"test\"}");

        assertThatThrownBy(() -> client.send(request))
                .isInstanceOf(JudgeRateLimitException.class)
                .hasMessageContaining("429");
    }

    @Test
    void shouldRetryOnServerError() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        AtomicInteger callCount = new AtomicInteger(0);

        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(500);
        when(errorResponse.body()).thenReturn("Server error");
        when(errorResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of(), (a, b) -> true));

        HttpResponse<String> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);
        when(successResponse.body()).thenReturn("{\"ok\": true}");
        when(successResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of(), (a, b) -> true));

        when(mockHttp.send(any(HttpRequest.class), any())).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() == 0) {
                return errorResponse;
            }
            return successResponse;
        });

        var client = new HttpJudgeClient(config, mockHttp) {
            @Override
            void sleep(Duration duration) {
                // no-op for testing
            }
        };

        var request = new HttpJudgeRequest(
                "https://test.api.com/v1/completions",
                Map.of(), "{\"prompt\": \"test\"}");

        HttpJudgeResponse response = client.send(request);
        assertThat(response.isSuccess()).isTrue();
        assertThat(callCount.get()).isEqualTo(2);
    }

    @Test
    void shouldThrowOnTimeout() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        doThrow(new java.net.http.HttpTimeoutException("timed out"))
                .when(mockHttp).send(any(HttpRequest.class), any());

        var client = new HttpJudgeClient(config, mockHttp);
        var request = new HttpJudgeRequest(
                "https://test.api.com/v1/completions",
                Map.of(), "{\"prompt\": \"test\"}");

        assertThatThrownBy(() -> client.send(request))
                .isInstanceOf(JudgeTimeoutException.class);
    }

    @Test
    void shouldThrowOnClientError() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("Bad request");
        when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of(), (a, b) -> true));
        doReturn(mockResponse).when(mockHttp).send(any(HttpRequest.class), any());

        var client = new HttpJudgeClient(config, mockHttp);
        var request = new HttpJudgeRequest(
                "https://test.api.com/v1/completions",
                Map.of(), "{\"prompt\": \"test\"}");

        assertThatThrownBy(() -> client.send(request))
                .isInstanceOf(JudgeException.class)
                .hasMessageContaining("400");
    }

    @Test
    void calculateDelayShouldRespectRetryAfterHeader() {
        var client = new HttpJudgeClient(config, mock(HttpClient.class));
        Duration delay = client.calculateDelay(0, "5");
        assertThat(delay).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void calculateDelayShouldUseExponentialBackoffWithoutHeader() {
        var client = new HttpJudgeClient(config, mock(HttpClient.class));
        Duration delay0 = client.calculateDelay(0, null);
        Duration delay1 = client.calculateDelay(1, null);

        assertThat(delay0.toMillis()).isGreaterThanOrEqualTo(500);
        assertThat(delay1.toMillis()).isGreaterThanOrEqualTo(1000);
    }
}
