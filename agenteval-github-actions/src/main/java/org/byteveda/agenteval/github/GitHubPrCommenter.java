package org.byteveda.agenteval.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Posts or updates evaluation results as a PR comment via the GitHub REST API.
 *
 * <p>Uses a hidden HTML marker ({@code <!-- agenteval-results -->}) to identify
 * and update existing comments instead of creating duplicates.</p>
 */
public final class GitHubPrCommenter {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubPrCommenter.class);
    private static final String MARKER = "<!-- agenteval-results -->";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String token;
    private final String apiUrl;
    private final HttpClient httpClient;

    /**
     * Creates a commenter for the given repository and PR.
     *
     * @param token  GitHub token (from GITHUB_TOKEN env var)
     * @param apiUrl base GitHub API URL (e.g., "https://api.github.com")
     */
    public GitHubPrCommenter(String token, String apiUrl) {
        this(token, apiUrl, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    GitHubPrCommenter(String token, String apiUrl, HttpClient httpClient) {
        this.token = Objects.requireNonNull(token, "token must not be null");
        this.apiUrl = Objects.requireNonNull(apiUrl, "apiUrl must not be null")
                .replaceAll("/$", "");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    /**
     * Posts or updates a comment on the specified PR.
     *
     * @param repo     repository in "owner/repo" format
     * @param prNumber the pull request number
     * @param body     the Markdown comment body
     */
    public void postOrUpdate(String repo, int prNumber, String body) throws IOException {
        String markedBody = MARKER + "\n" + body;
        String commentsUrl = apiUrl + "/repos/" + repo + "/issues/" + prNumber + "/comments";

        // Check for existing comment with our marker
        Long existingCommentId = findExistingComment(commentsUrl);

        if (existingCommentId != null) {
            updateComment(commentsUrl, existingCommentId, markedBody);
            LOG.info("Updated existing AgentEval comment on PR #{}", prNumber);
        } else {
            createComment(commentsUrl, markedBody);
            LOG.info("Created new AgentEval comment on PR #{}", prNumber);
        }
    }

    private Long findExistingComment(String commentsUrl) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(commentsUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                LOG.warn("Failed to list comments (status {})", response.statusCode());
                return null;
            }

            JsonNode comments = MAPPER.readTree(response.body());
            for (JsonNode comment : comments) {
                String commentBody = comment.path("body").asText("");
                if (commentBody.contains(MARKER)) {
                    return comment.path("id").asLong();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while listing comments", e);
        }

        return null;
    }

    private void createComment(String commentsUrl, String body) throws IOException {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("body", body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(commentsUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        MAPPER.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        sendRequest(request, "create comment");
    }

    private void updateComment(String commentsUrl, long commentId, String body) throws IOException {
        // PATCH /repos/{owner}/{repo}/issues/comments/{comment_id}
        String patchUrl = apiUrl + "/repos/"
                + extractRepo(commentsUrl) + "/issues/comments/" + commentId;

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("body", body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(patchUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(
                        MAPPER.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        sendRequest(request, "update comment");
    }

    private void sendRequest(HttpRequest request, String action) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 300) {
                LOG.error("Failed to {} (status {}): {}", action,
                        response.statusCode(), response.body());
                throw new IOException("GitHub API " + action + " failed with status "
                        + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during " + action, e);
        }
    }

    private static String extractRepo(String commentsUrl) {
        // commentsUrl = https://api.github.com/repos/owner/repo/issues/123/comments
        String[] parts = commentsUrl.split("/repos/")[1].split("/issues/");
        return parts[0];
    }
}
