package org.byteveda.agenteval.replay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A {@link Function} that replays recorded agent interactions in sequence.
 *
 * <p>Returns recorded outputs in the order they were captured, using an
 * {@link AtomicInteger} cursor for thread-safe sequential access.
 * Throws {@link ReplayMismatchException} if all interactions are exhausted.</p>
 */
public final class ReplayAgentWrapper implements Function<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(ReplayAgentWrapper.class);

    private final List<RecordedInteraction> agentInteractions;
    private final AtomicInteger cursor;

    /**
     * Creates a replay agent from a recording.
     *
     * @param recording the recording containing agent interactions to replay
     */
    public ReplayAgentWrapper(Recording recording) {
        Objects.requireNonNull(recording, "recording must not be null");
        this.agentInteractions = recording.agentInteractions();
        this.cursor = new AtomicInteger(0);
    }

    @Override
    public String apply(String input) {
        int index = cursor.getAndIncrement();
        if (index >= agentInteractions.size()) {
            throw new ReplayMismatchException(
                    "Replay exhausted: requested interaction index " + index
                            + " but only " + agentInteractions.size()
                            + " agent interactions were recorded");
        }

        RecordedInteraction interaction = agentInteractions.get(index);
        LOG.debug("Replaying agent interaction {}/{} (input length={})",
                index + 1, agentInteractions.size(), input.length());

        return interaction.output();
    }

    /**
     * Returns the current cursor position (number of interactions replayed so far).
     */
    public int position() {
        return cursor.get();
    }

    /**
     * Returns the total number of recorded agent interactions available.
     */
    public int totalInteractions() {
        return agentInteractions.size();
    }
}
