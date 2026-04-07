package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.model.TokenUsage;

import java.util.Objects;

/**
 * A single recorded LLM interaction (agent or judge).
 *
 * @param type        whether this was an agent or judge interaction
 * @param input       the prompt / input sent to the model
 * @param output      the response received from the model
 * @param tokenUsage  token usage for the interaction (may be null)
 * @param timestampMs epoch millis when the interaction occurred
 */
public record RecordedInteraction(
        InteractionType type,
        String input,
        String output,
        TokenUsage tokenUsage,
        long timestampMs
) {
    public RecordedInteraction {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(output, "output must not be null");
    }
}
