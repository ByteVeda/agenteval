package com.agenteval.judge.parse;

import com.agenteval.judge.JudgeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts score and reason from LLM judge responses.
 *
 * <p>Three-tier extraction strategy:
 * <ol>
 *   <li>Parse as clean JSON object with "score" and "reason" fields</li>
 *   <li>Regex extract {@code {"score":..., "reason":...}} from mixed text</li>
 *   <li>Extract bare numeric score (0.0–1.0) from text</li>
 * </ol>
 */
public final class JudgeResponseParser {

    private static final Logger LOG = LoggerFactory.getLogger(JudgeResponseParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern JSON_BLOCK = Pattern.compile(
            "\\{[^{}]*\"score\"\\s*:\\s*([\\d.]+)[^{}]*}",
            Pattern.DOTALL);

    private static final Pattern SCORE_FIELD = Pattern.compile(
            "\"score\"\\s*:\\s*([\\d.]+)");

    private static final Pattern REASON_FIELD = Pattern.compile(
            "\"reason\"\\s*:\\s*\"([^\"]*?)\"");

    private static final Pattern BARE_SCORE = Pattern.compile(
            "\\b(0(?:\\.\\d+)?|1(?:\\.0+)?)\\b");

    private JudgeResponseParser() {}

    /**
     * Parsed result from a judge response.
     */
    public record ParsedScore(double score, String reason) {}

    /**
     * Parses score and reason from the raw LLM response text.
     *
     * @throws JudgeException if no score can be extracted
     */
    public static ParsedScore parse(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            throw new JudgeException("Empty judge response");
        }

        // Tier 1: clean JSON
        ParsedScore fromJson = tryParseJson(responseText.trim());
        if (fromJson != null) {
            LOG.debug("Parsed score from clean JSON: {}", fromJson.score());
            return fromJson;
        }

        // Tier 2: regex JSON block
        ParsedScore fromRegex = tryRegexJsonBlock(responseText);
        if (fromRegex != null) {
            LOG.debug("Parsed score from regex JSON block: {}", fromRegex.score());
            return fromRegex;
        }

        // Tier 3: bare numeric score
        ParsedScore fromBare = tryBareScore(responseText);
        if (fromBare != null) {
            LOG.debug("Parsed bare score: {}", fromBare.score());
            return fromBare;
        }

        throw new JudgeException(
                "Could not extract score from judge response: "
                        + responseText.substring(0, Math.min(responseText.length(), 200)));
    }

    private static ParsedScore tryParseJson(String text) {
        try {
            JsonNode root = MAPPER.readTree(text);
            if (root.has("score")) {
                double score = root.get("score").asDouble();
                String reason = root.has("reason") ? root.get("reason").asText() : "";
                return validated(score, reason);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Not clean JSON, fall through
        }
        return null;
    }

    private static ParsedScore tryRegexJsonBlock(String text) {
        Matcher blockMatcher = JSON_BLOCK.matcher(text);
        if (blockMatcher.find()) {
            String block = blockMatcher.group();
            Matcher scoreMatcher = SCORE_FIELD.matcher(block);
            if (scoreMatcher.find()) {
                try {
                    double score = Double.parseDouble(scoreMatcher.group(1));
                    Matcher reasonMatcher = REASON_FIELD.matcher(block);
                    String reason = reasonMatcher.find() ? reasonMatcher.group(1) : "";
                    return validated(score, reason);
                } catch (NumberFormatException e) {
                    // Fall through
                }
            }
        }
        return null;
    }

    private static ParsedScore tryBareScore(String text) {
        Matcher matcher = BARE_SCORE.matcher(text);
        while (matcher.find()) {
            try {
                double score = Double.parseDouble(matcher.group(1));
                if (score >= 0.0 && score <= 1.0) {
                    return new ParsedScore(score, "");
                }
            } catch (NumberFormatException e) {
                // Try next match
            }
        }
        return null;
    }

    private static ParsedScore validated(double score, String reason) {
        if (score < 0.0 || score > 1.0) {
            return null;
        }
        return new ParsedScore(score, reason != null ? reason : "");
    }
}
