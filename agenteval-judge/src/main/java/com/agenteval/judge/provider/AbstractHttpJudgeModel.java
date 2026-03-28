package com.agenteval.judge.provider;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.model.TokenUsage;
import com.agenteval.judge.JudgeException;
import com.agenteval.judge.config.JudgeConfig;
import com.agenteval.judge.http.HttpJudgeClient;
import com.agenteval.judge.http.HttpJudgeRequest;
import com.agenteval.judge.http.HttpJudgeResponse;
import com.agenteval.judge.parse.JudgeResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for HTTP-based judge model providers.
 *
 * <p>Implements the template method pattern: {@link #judge(String)} is final and
 * delegates to {@link #buildRequest(String)} and {@link #parseResponse(HttpJudgeResponse)}
 * which subclasses implement for provider-specific formats.</p>
 */
public abstract sealed class AbstractHttpJudgeModel implements JudgeModel
        permits OpenAiJudgeModel, AnthropicJudgeModel, GoogleJudgeModel,
                OllamaJudgeModel, AzureOpenAiJudgeModel, BedrockJudgeModel,
                CustomHttpJudgeModel {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractHttpJudgeModel.class);

    protected final JudgeConfig config;
    private final HttpJudgeClient client;

    protected AbstractHttpJudgeModel(JudgeConfig config) {
        this.config = config;
        this.client = new HttpJudgeClient(config);
    }

    protected AbstractHttpJudgeModel(JudgeConfig config, HttpJudgeClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public final JudgeResponse judge(String prompt) {
        LOG.debug("Sending judge request to {} (model: {})", config.getBaseUrl(), modelId());
        HttpJudgeRequest request = buildRequest(prompt);
        HttpJudgeResponse httpResponse = client.send(request);
        return parseResponse(httpResponse);
    }

    @Override
    public String modelId() {
        return config.getModel();
    }

    /**
     * Builds the provider-specific HTTP request for the given prompt.
     */
    protected abstract HttpJudgeRequest buildRequest(String prompt);

    /**
     * Parses the provider-specific HTTP response into a JudgeResponse.
     * Default implementation uses {@link JudgeResponseParser} to extract the score
     * from the LLM's text output, plus provider-specific token usage.
     */
    protected JudgeResponse parseResponse(HttpJudgeResponse httpResponse) {
        String content = extractContent(httpResponse.body());
        TokenUsage tokenUsage = extractTokenUsage(httpResponse.body());

        JudgeResponseParser.ParsedScore parsed = JudgeResponseParser.parse(content);
        return new JudgeResponse(parsed.score(), parsed.reason(), tokenUsage);
    }

    /**
     * Extracts the assistant's text content from the provider-specific response body.
     */
    protected abstract String extractContent(String responseBody);

    /**
     * Extracts token usage from the provider-specific response body.
     * Returns null if not available.
     */
    protected abstract TokenUsage extractTokenUsage(String responseBody);

    /**
     * Parses JSON safely, throwing JudgeException on failure.
     */
    protected static com.fasterxml.jackson.databind.JsonNode parseJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new JudgeException("Failed to parse judge response JSON: " + e.getMessage(), e);
        }
    }
}
