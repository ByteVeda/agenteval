package org.byteveda.agenteval.github;

import org.byteveda.agenteval.core.eval.EvalResult;
import org.byteveda.agenteval.reporting.EvalReporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point for the GitHub Actions integration.
 *
 * <p>Reads environment variables, loads configuration and dataset, runs the evaluation,
 * generates a Markdown report, optionally posts a PR comment, and exits non-zero on failure.</p>
 *
 * <p>Environment variables:</p>
 * <ul>
 *   <li>{@code INPUT_DATASET_PATH} — path to the dataset file (required)</li>
 *   <li>{@code INPUT_CONFIG_FILE} — path to agenteval.yaml (default: agenteval.yaml)</li>
 *   <li>{@code INPUT_FAIL_ON_REGRESSION} — exit non-zero if any test fails (default: false)</li>
 *   <li>{@code INPUT_POST_PR_COMMENT} — post results as PR comment (default: true)</li>
 *   <li>{@code GITHUB_TOKEN} — GitHub token for PR commenting</li>
 *   <li>{@code GITHUB_REPOSITORY} — "owner/repo"</li>
 *   <li>{@code GITHUB_EVENT_PATH} — path to event JSON (to extract PR number)</li>
 *   <li>{@code GITHUB_API_URL} — GitHub API base URL</li>
 *   <li>{@code GITHUB_OUTPUT} — path to set action outputs</li>
 * </ul>
 */
public final class GitHubActionRunner {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubActionRunner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GitHubActionRunner() {}

    @SuppressWarnings({"IllegalCatch", "SystemExitOutsideMain"})
    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            LOG.error("AgentEval GitHub Action failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    static void run() throws Exception {
        String datasetPath = requireEnv("INPUT_DATASET_PATH");
        String configFile = envOrDefault("INPUT_CONFIG_FILE", "agenteval.yaml");
        boolean failOnRegression = Boolean.parseBoolean(
                envOrDefault("INPUT_FAIL_ON_REGRESSION", "false"));
        boolean postPrComment = Boolean.parseBoolean(
                envOrDefault("INPUT_POST_PR_COMMENT", "true"));

        LOG.info("AgentEval GitHub Action starting");
        LOG.info("  Dataset: {}", datasetPath);
        LOG.info("  Config: {}", configFile);

        // Load and run evaluation result from report JSON if available,
        // or run directly. For now, we load from a pre-generated JSON report.
        Path reportJsonPath = Path.of(
                envOrDefault("INPUT_REPORT_JSON", "target/agenteval/agenteval-report.json"));

        if (!Files.exists(reportJsonPath)) {
            throw new IllegalStateException(
                    "Report JSON not found at " + reportJsonPath
                            + ". Run 'mvn verify' with agenteval-maven-plugin first.");
        }

        EvalResult result = loadResult(reportJsonPath);
        LOG.info("Loaded evaluation results: {} cases, {:.1f}% pass rate",
                result.caseResults().size(), result.passRate() * 100);

        // Generate Markdown report
        Path mdPath = Path.of("agenteval-report.md");
        MarkdownConfig mdConfig = MarkdownConfig.builder()
                .outputPath(mdPath)
                .includeFailedDetails(true)
                .includeMetricBreakdown(true)
                .build();
        EvalReporter mdReporter = new MarkdownReporter(mdConfig);
        mdReporter.report(result);
        LOG.info("Markdown report written to {}", mdPath);

        // Post PR comment
        if (postPrComment) {
            postComment(result, mdConfig);
        }

        // Set GitHub Action outputs
        setOutput("pass-rate", String.format("%.1f", result.passRate() * 100));
        setOutput("average-score", String.format("%.3f", result.averageScore()));
        setOutput("total-cases", String.valueOf(result.caseResults().size()));
        setOutput("failed-cases", String.valueOf(result.failedCases().size()));

        // Fail if regression
        if (failOnRegression && !result.failedCases().isEmpty()) {
            throw new RuntimeException(String.format(
                    "Evaluation regression: %d/%d cases failed",
                    result.failedCases().size(), result.caseResults().size()));
        }

        LOG.info("AgentEval GitHub Action completed successfully");
    }

    private static void postComment(EvalResult result, MarkdownConfig mdConfig) {
        String token = System.getenv("GITHUB_TOKEN");
        String repo = System.getenv("GITHUB_REPOSITORY");
        String apiUrl = envOrDefault("GITHUB_API_URL", "https://api.github.com");

        if (token == null || repo == null) {
            LOG.warn("GITHUB_TOKEN or GITHUB_REPOSITORY not set, skipping PR comment");
            return;
        }

        int prNumber = extractPrNumber();
        if (prNumber <= 0) {
            LOG.warn("Could not determine PR number, skipping PR comment");
            return;
        }

        try {
            MarkdownReporter renderer = new MarkdownReporter(mdConfig);
            String body = renderer.render(result);
            new GitHubPrCommenter(token, apiUrl).postOrUpdate(repo, prNumber, body);
        } catch (IOException e) {
            LOG.error("Failed to post PR comment: {}", e.getMessage());
        }
    }

    @SuppressWarnings("IllegalCatch")
    private static int extractPrNumber() {
        String eventPath = System.getenv("GITHUB_EVENT_PATH");
        if (eventPath == null) return -1;

        try {
            var eventJson = MAPPER.readTree(Path.of(eventPath).toFile());
            return eventJson.path("pull_request").path("number").asInt(-1);
        } catch (Exception e) {
            LOG.debug("Could not extract PR number from event: {}", e.getMessage());
            return -1;
        }
    }

    private static EvalResult loadResult(Path jsonPath) throws IOException {
        return MAPPER.readValue(jsonPath.toFile(), EvalResult.class);
    }

    private static void setOutput(String name, String value) {
        String outputFile = System.getenv("GITHUB_OUTPUT");
        if (outputFile != null) {
            try {
                Files.writeString(Path.of(outputFile),
                        name + "=" + value + "\n",
                        java.nio.charset.StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.APPEND,
                        java.nio.file.StandardOpenOption.CREATE);
            } catch (IOException e) {
                LOG.debug("Could not write GitHub output: {}", e.getMessage());
            }
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
