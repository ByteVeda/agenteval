package org.byteveda.agenteval.github;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitHubPrCommenterTest {

    @SuppressWarnings("unchecked")
    @Test
    void shouldCreateNewCommentWhenNoneExists() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> listResponse = mock(HttpResponse.class);
        when(listResponse.statusCode()).thenReturn(200);
        when(listResponse.body()).thenReturn("[]");

        HttpResponse<String> createResponse = mock(HttpResponse.class);
        when(createResponse.statusCode()).thenReturn(201);
        when(createResponse.body()).thenReturn("{\"id\":1}");

        List<HttpRequest> requests = new ArrayList<>();
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenAnswer(invocation -> {
                    HttpRequest req = invocation.getArgument(0);
                    requests.add(req);
                    // First call = list, second call = create
                    if (requests.size() == 1) return listResponse;
                    return createResponse;
                });

        var commenter = new GitHubPrCommenter("test-token",
                "https://api.github.com", mockClient);
        commenter.postOrUpdate("owner/repo", 42, "Test results");

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).method()).isEqualTo("GET");
        assertThat(requests.get(1).method()).isEqualTo("POST");
        assertThat(requests.get(0).uri().toString())
                .contains("/repos/owner/repo/issues/42/comments");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldUpdateExistingCommentWithMarker() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> listResponse = mock(HttpResponse.class);
        when(listResponse.statusCode()).thenReturn(200);
        when(listResponse.body()).thenReturn(
                "[{\"id\":99,\"body\":\"<!-- agenteval-results -->\\nOld results\"}]");

        HttpResponse<String> updateResponse = mock(HttpResponse.class);
        when(updateResponse.statusCode()).thenReturn(200);
        when(updateResponse.body()).thenReturn("{\"id\":99}");

        List<HttpRequest> requests = new ArrayList<>();
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenAnswer(invocation -> {
                    HttpRequest req = invocation.getArgument(0);
                    requests.add(req);
                    if (requests.size() == 1) return listResponse;
                    return updateResponse;
                });

        var commenter = new GitHubPrCommenter("test-token",
                "https://api.github.com", mockClient);
        commenter.postOrUpdate("owner/repo", 42, "New results");

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).method()).isEqualTo("GET");
        assertThat(requests.get(1).method()).isEqualTo("PATCH");
        assertThat(requests.get(1).uri().toString()).contains("/issues/comments/99");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldThrowOnCreateFailure() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> listResponse = mock(HttpResponse.class);
        when(listResponse.statusCode()).thenReturn(200);
        when(listResponse.body()).thenReturn("[]");

        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(403);
        when(errorResponse.body()).thenReturn("{\"message\":\"Forbidden\"}");

        List<HttpResponse<String>> responses = List.of(listResponse, errorResponse);
        var callCount = new int[]{0};
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenAnswer(invocation -> responses.get(callCount[0]++));

        var commenter = new GitHubPrCommenter("bad-token",
                "https://api.github.com", mockClient);

        assertThatThrownBy(() -> commenter.postOrUpdate("owner/repo", 42, "Results"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("failed with status 403");
    }

    @Test
    void shouldIncludeAuthorizationHeader() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);

        @SuppressWarnings("unchecked")
        HttpResponse<String> listResponse = mock(HttpResponse.class);
        when(listResponse.statusCode()).thenReturn(200);
        when(listResponse.body()).thenReturn("[]");

        @SuppressWarnings("unchecked")
        HttpResponse<String> createResponse = mock(HttpResponse.class);
        when(createResponse.statusCode()).thenReturn(201);
        when(createResponse.body()).thenReturn("{\"id\":1}");

        List<HttpRequest> requests = new ArrayList<>();
        when(mockClient.send(any(HttpRequest.class), any()))
                .thenAnswer(invocation -> {
                    requests.add(invocation.getArgument(0));
                    if (requests.size() == 1) return listResponse;
                    return createResponse;
                });

        var commenter = new GitHubPrCommenter("my-secret-token",
                "https://api.github.com", mockClient);
        commenter.postOrUpdate("owner/repo", 1, "Results");

        for (HttpRequest req : requests) {
            assertThat(req.headers().firstValue("Authorization"))
                    .hasValue("Bearer my-secret-token");
        }
    }
}
