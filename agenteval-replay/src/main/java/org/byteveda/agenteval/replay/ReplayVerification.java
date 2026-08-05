package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.model.EvalScore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The result of replaying a recording and comparing metric scores.
 *
 * @param recordingName       the name of the recording that was replayed
 * @param originalScores      metric scores from the original run
 * @param replayedScores      metric scores from the replay run
 * @param allMatch            true if all replayed scores match the originals
 * @param mismatches          descriptions of any score mismatches
 */
public record ReplayVerification(
        String recordingName,
        Map<String, EvalScore> originalScores,
        Map<String, EvalScore> replayedScores,
        boolean allMatch,
        List<String> mismatches
) {
    public ReplayVerification {
        Objects.requireNonNull(recordingName, "recordingName must not be null");
        originalScores = originalScores == null
                ? Map.of() : Map.copyOf(originalScores);
        replayedScores = replayedScores == null
                ? Map.of() : Map.copyOf(replayedScores);
        mismatches = mismatches == null
                ? List.of() : List.copyOf(mismatches);
    }
}
