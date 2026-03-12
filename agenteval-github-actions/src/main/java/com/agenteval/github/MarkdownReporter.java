package com.agenteval.github;

import com.agenteval.core.eval.CaseResult;
import com.agenteval.core.eval.EvalResult;
import com.agenteval.core.model.EvalScore;
import com.agenteval.reporting.EvalReporter;
import com.agenteval.reporting.ReportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

/**
 * Generates GitHub-Flavored Markdown evaluation reports.
 *
 * <p>Produces GFM tables with metric averages, pass rates, and optional
 * failed case details in collapsible {@code <details>} tags.</p>
 */
public final class MarkdownReporter implements EvalReporter {

    private static final Logger LOG = LoggerFactory.getLogger(MarkdownReporter.class);

    private final MarkdownConfig config;

    public MarkdownReporter(MarkdownConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    @Override
    public void report(EvalResult result) {
        LOG.debug("Generating Markdown report at {}", config.outputPath());
        String markdown = render(result);
        try {
            Files.writeString(config.outputPath(), markdown, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReportException(
                    "Failed to write Markdown report to " + config.outputPath(), e);
        }
    }

    /**
     * Renders the evaluation result as a Markdown string.
     */
    public String render(EvalResult result) {
        var sb = new StringBuilder();

        sb.append("## AgentEval Results\n\n");

        // Summary table
        int total = result.caseResults().size();
        int failed = result.failedCases().size();
        int passed = total - failed;
        String passRateStr = String.format("%.1f%%", result.passRate() * 100);
        String avgScoreStr = String.format("%.3f", result.averageScore());
        String statusIcon = failed == 0 ? "✅" : "❌";

        sb.append("| Status | Cases | Passed | Failed | Pass Rate | Avg Score | Duration |\n");
        sb.append("|--------|-------|--------|--------|-----------|-----------|----------|\n");
        sb.append(String.format("| %s | %d | %d | %d | %s | %s | %dms |\n\n",
                statusIcon, total, passed, failed, passRateStr, avgScoreStr,
                result.durationMs()));

        // Per-metric breakdown
        if (config.includeMetricBreakdown()) {
            Map<String, Double> byMetric = result.averageScoresByMetric();
            if (!byMetric.isEmpty()) {
                sb.append("### Per-Metric Averages\n\n");
                sb.append("| Metric | Average Score | Status |\n");
                sb.append("|--------|---------------|--------|\n");
                byMetric.forEach((name, avg) -> {
                    String icon = avg >= 0.5 ? "✅" : "❌";
                    sb.append(String.format("| %s | %.3f | %s |\n", name, avg, icon));
                });
                sb.append("\n");
            }
        }

        // Failed case details
        if (config.includeFailedDetails() && !result.failedCases().isEmpty()) {
            sb.append("<details>\n");
            sb.append("<summary><strong>Failed Cases (")
              .append(failed).append(")</strong></summary>\n\n");

            for (CaseResult cr : result.failedCases()) {
                String input = truncate(cr.testCase().getInput(), 80);
                sb.append("#### ").append(escapeMarkdown(input)).append("\n\n");
                sb.append("| Metric | Score | Threshold | Reason |\n");
                sb.append("|--------|-------|-----------|--------|\n");
                for (EvalScore score : cr.failedScores()) {
                    sb.append(String.format("| %s | %.2f | %.2f | %s |\n",
                            score.metricName(), score.value(),
                            score.threshold(), escapeMarkdown(score.reason())));
                }
                sb.append("\n");
            }

            sb.append("</details>\n");
        }

        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static String escapeMarkdown(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ");
    }
}
