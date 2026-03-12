package com.agenteval.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;
import java.util.Objects;

/**
 * Represents a multi-turn conversation evaluation test case.
 */
@JsonDeserialize(builder = ConversationTestCase.Builder.class)
public final class ConversationTestCase {

    private final List<AgentTestCase> turns;
    private final String conversationId;
    private final String systemPrompt;

    private ConversationTestCase(Builder builder) {
        Objects.requireNonNull(builder.turns, "turns must not be null");
        if (builder.turns.isEmpty()) {
            throw new IllegalArgumentException("turns must not be empty");
        }
        this.turns = List.copyOf(builder.turns);
        this.conversationId = builder.conversationId;
        this.systemPrompt = builder.systemPrompt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<AgentTestCase> getTurns() { return turns; }
    public String getConversationId() { return conversationId; }
    public String getSystemPrompt() { return systemPrompt; }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private List<AgentTestCase> turns;
        private String conversationId;
        private String systemPrompt;

        private Builder() {}

        public Builder turns(List<AgentTestCase> turns) { this.turns = turns; return this; }
        public Builder conversationId(String conversationId) { this.conversationId = conversationId; return this; }
        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }

        public ConversationTestCase build() {
            return new ConversationTestCase(this);
        }
    }
}
