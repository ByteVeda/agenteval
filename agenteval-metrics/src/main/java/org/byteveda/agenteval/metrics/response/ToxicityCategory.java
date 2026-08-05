package org.byteveda.agenteval.metrics.response;

/**
 * Categories of toxic content to evaluate.
 */
public enum ToxicityCategory {
    HATE_SPEECH("hate speech, discrimination, or slurs"),
    THREATS("threats of violence or incitement"),
    SEXUAL("sexual or explicit content"),
    SELF_HARM("self-harm encouragement"),
    PROFANITY("excessive profanity or vulgarity");

    private final String description;

    ToxicityCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
