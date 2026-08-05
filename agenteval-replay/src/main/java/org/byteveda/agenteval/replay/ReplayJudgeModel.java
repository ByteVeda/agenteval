package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * A {@link JudgeModel} that replays recorded judge interactions instead of
 * making live LLM calls.
 *
 * <p>Matches incoming prompts against the recorded interactions by exact string
 * match. Throws {@link ReplayMismatchException} if no matching recording is found.</p>
 *
 * <p>This enables deterministic, cost-free re-evaluation of previously recorded
 * evaluation runs.</p>
 */
public final class ReplayJudgeModel implements JudgeModel {

    private static final Logger LOG = LoggerFactory.getLogger(ReplayJudgeModel.class);

    private final List<RecordedInteraction> judgeInteractions;
    private final String modelId;

    /**
     * Creates a replay judge from a recording.
     *
     * @param recording the recording containing judge interactions to replay
     * @param modelId   the model identifier to report
     */
    public ReplayJudgeModel(Recording recording, String modelId) {
        Objects.requireNonNull(recording, "recording must not be null");
        this.modelId = Objects.requireNonNull(modelId, "modelId must not be null");
        this.judgeInteractions = recording.judgeInteractions();
    }

    @Override
    public JudgeResponse judge(String prompt) {
        for (RecordedInteraction interaction : judgeInteractions) {
            if (interaction.input().equals(prompt)) {
                LOG.debug("Replay hit for judge prompt (length={})", prompt.length());
                return parseResponse(interaction);
            }
        }

        throw new ReplayMismatchException(
                "No recorded judge interaction found for prompt (length="
                        + prompt.length() + "): "
                        + prompt.substring(0, Math.min(200, prompt.length())) + "...");
    }

    @Override
    public String modelId() {
        return modelId;
    }

    private static JudgeResponse parseResponse(RecordedInteraction interaction) {
        String output = interaction.output();
        int separator = output.indexOf('|');
        if (separator < 0) {
            throw new ReplayMismatchException(
                    "Malformed recorded judge output (missing '|' separator): " + output);
        }

        double score = Double.parseDouble(output.substring(0, separator));
        String reason = output.substring(separator + 1);
        return new JudgeResponse(score, reason, interaction.tokenUsage());
    }
}
