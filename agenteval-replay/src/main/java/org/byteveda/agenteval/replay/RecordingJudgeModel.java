package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link JudgeModel} decorator that records all judge interactions.
 *
 * <p>Delegates to the underlying judge model and captures each prompt/response
 * pair as a {@link RecordedInteraction}. Thread-safe via {@link CopyOnWriteArrayList}.</p>
 *
 * <pre>{@code
 * JudgeModel delegate = new OpenAiJudgeModel(...);
 * RecordingJudgeModel recording = new RecordingJudgeModel(delegate);
 * // use recording as the judge — all calls are captured
 * List<RecordedInteraction> captured = recording.getInteractions();
 * }</pre>
 */
public final class RecordingJudgeModel implements JudgeModel {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingJudgeModel.class);

    private final JudgeModel delegate;
    private final CopyOnWriteArrayList<RecordedInteraction> interactions;

    public RecordingJudgeModel(JudgeModel delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.interactions = new CopyOnWriteArrayList<>();
    }

    @Override
    public JudgeResponse judge(String prompt) {
        JudgeResponse response = delegate.judge(prompt);

        TokenUsage tokenUsage = response.tokenUsage();
        String output = response.score() + "|" + response.reason();

        var interaction = new RecordedInteraction(
                InteractionType.JUDGE,
                prompt,
                output,
                tokenUsage,
                System.currentTimeMillis()
        );
        interactions.add(interaction);

        LOG.debug("Recorded judge interaction (prompt length={}, score={})",
                prompt.length(), response.score());

        return response;
    }

    @Override
    public String modelId() {
        return delegate.modelId();
    }

    /**
     * Returns an unmodifiable snapshot of all recorded judge interactions.
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
