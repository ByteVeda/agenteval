package com.agenteval.core.judge;

/**
 * SPI interface for LLM-as-judge providers.
 *
 * <p>Implementations are provided by {@code agenteval-judge} module
 * (OpenAI, Anthropic, Ollama, etc.). Users can implement this interface
 * for custom HTTP-compatible LLM endpoints.</p>
 */
public interface JudgeModel {

    /**
     * Sends a prompt to the judge LLM and returns the evaluation response.
     *
     * @param prompt the evaluation prompt (metric-specific)
     * @return the judge's response with score and reasoning
     */
    JudgeResponse judge(String prompt);

    /**
     * Returns the model identifier (e.g., "gpt-4o", "claude-sonnet-4-20250514").
     */
    String modelId();
}
