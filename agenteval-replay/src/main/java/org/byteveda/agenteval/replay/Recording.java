package org.byteveda.agenteval.replay;

import java.util.List;
import java.util.Objects;

/**
 * An immutable collection of recorded interactions from an evaluation run.
 *
 * @param name         a human-readable identifier for this recording
 * @param interactions the ordered list of all recorded interactions
 * @param recordedAtMs epoch millis when the recording was created
 */
public record Recording(
        String name,
        List<RecordedInteraction> interactions,
        long recordedAtMs
) {
    public Recording {
        Objects.requireNonNull(name, "name must not be null");
        interactions = interactions == null ? List.of() : List.copyOf(interactions);
    }

    /**
     * Returns only agent interactions, preserving order.
     */
    public List<RecordedInteraction> agentInteractions() {
        return interactions.stream()
                .filter(i -> i.type() == InteractionType.AGENT)
                .toList();
    }

    /**
     * Returns only judge interactions, preserving order.
     */
    public List<RecordedInteraction> judgeInteractions() {
        return interactions.stream()
                .filter(i -> i.type() == InteractionType.JUDGE)
                .toList();
    }
}
