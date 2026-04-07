package org.byteveda.agenteval.replay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A {@link Function} decorator that records all agent interactions.
 *
 * <p>Wraps an agent function ({@code String -> String}) and captures each
 * input/output pair as a {@link RecordedInteraction}. Thread-safe via
 * {@link CopyOnWriteArrayList}.</p>
 *
 * <pre>{@code
 * Function<String, String> agent = input -> myAgent.call(input);
 * RecordingAgentWrapper recording = new RecordingAgentWrapper(agent);
 * String result = recording.apply("What is Java?");
 * List<RecordedInteraction> captured = recording.getInteractions();
 * }</pre>
 */
public final class RecordingAgentWrapper implements Function<String, String> {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingAgentWrapper.class);

    private final Function<String, String> delegate;
    private final CopyOnWriteArrayList<RecordedInteraction> interactions;

    public RecordingAgentWrapper(Function<String, String> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.interactions = new CopyOnWriteArrayList<>();
    }

    @Override
    public String apply(String input) {
        String output = delegate.apply(input);

        var interaction = new RecordedInteraction(
                InteractionType.AGENT,
                input,
                output != null ? output : "",
                null,
                System.currentTimeMillis()
        );
        interactions.add(interaction);

        LOG.debug("Recorded agent interaction (input length={}, output length={})",
                input.length(), output != null ? output.length() : 0);

        return output;
    }

    /**
     * Returns an unmodifiable snapshot of all recorded agent interactions.
     */
    public List<RecordedInteraction> getInteractions() {
        return List.copyOf(interactions);
    }

    /**
     * Clears all recorded interactions.
     */
    public void clear() {
        interactions.clear();
    }

    /**
     * Returns the number of recorded interactions.
     */
    public int size() {
        return interactions.size();
    }
}
