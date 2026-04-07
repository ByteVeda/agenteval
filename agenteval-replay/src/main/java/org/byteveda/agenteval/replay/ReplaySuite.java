package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Orchestrates recording and replaying of agent evaluation runs.
 *
 * <p>In <b>record mode</b>, the suite wraps the agent and judge in recording
 * decorators, runs the evaluation, and persists the interactions via
 * {@link RecordingStore}.</p>
 *
 * <p>In <b>replay mode</b>, the suite loads a previously saved recording and
 * replays both agent and judge interactions deterministically, then verifies
 * that metric scores match.</p>
 *
 * <pre>{@code
 * ReplaySuite suite = ReplaySuite.builder()
 *     .agent(myAgent::call)
 *     .judgeModel(openAiJudge)
 *     .metric(answerRelevancy)
 *     .testCase(testCase)
 *     .recordingStore(new RecordingStore(Path.of("recordings")))
 *     .recordingName("baseline-v1")
 *     .build();
 *
 * // Record a run
 * Recording recording = suite.record();
 *
 * // Replay and verify
 * ReplayVerification verification = suite.replay();
 * assert verification.allMatch();
 * }</pre>
 */
public final class ReplaySuite {

    private static final Logger LOG = LoggerFactory.getLogger(ReplaySuite.class);

    private final Function<String, String> agent;
    private final JudgeModel judgeModel;
    private final List<EvalMetric> metrics;
    private final List<AgentTestCase> testCases;
    private final RecordingStore recordingStore;
    private final String recordingName;

    private ReplaySuite(Builder builder) {
        this.agent = Objects.requireNonNull(builder.agent,
                "agent must not be null");
        this.judgeModel = Objects.requireNonNull(builder.judgeModel,
                "judgeModel must not be null");
        this.metrics = List.copyOf(builder.metrics);
        this.testCases = List.copyOf(builder.testCases);
        this.recordingStore = Objects.requireNonNull(builder.recordingStore,
                "recordingStore must not be null");
        this.recordingName = Objects.requireNonNull(builder.recordingName,
                "recordingName must not be null");

        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("at least one metric is required");
        }
        if (testCases.isEmpty()) {
            throw new IllegalArgumentException("at least one test case is required");
        }
    }

    /**
     * Records an evaluation run: invokes the agent and judge for each test case,
     * captures all interactions, and saves the recording to the store.
     *
     * @return the saved recording
     */
    public Recording record() {
        LOG.info("Recording evaluation run '{}' with {} test case(s) and {} metric(s)",
                recordingName, testCases.size(), metrics.size());

        var recordingAgent = new RecordingAgentWrapper(agent);
        var recordingJudge = new RecordingJudgeModel(judgeModel);

        Map<String, EvalScore> scores = runEvaluation(
                recordingAgent, recordingJudge);

        List<RecordedInteraction> allInteractions = new ArrayList<>();
        allInteractions.addAll(recordingAgent.getInteractions());
        allInteractions.addAll(recordingJudge.getInteractions());

        var recording = new Recording(
                recordingName,
                allInteractions,
                System.currentTimeMillis()
        );

        recordingStore.save(recording);

        LOG.info("Recording '{}' saved: {} agent + {} judge interactions, {} scores",
                recordingName,
                recordingAgent.size(),
                recordingJudge.size(),
                scores.size());

        return recording;
    }

    /**
     * Replays a previously recorded evaluation run and verifies that metric
     * scores are deterministic.
     *
     * @return the verification result comparing original and replayed scores
     * @throws ReplayMismatchException if the recording cannot be found
     */
    public ReplayVerification replay() {
        LOG.info("Replaying recording '{}'", recordingName);

        Recording recording = recordingStore.load(recordingName)
                .orElseThrow(() -> new ReplayMismatchException(
                        "Recording not found: " + recordingName));

        // First pass: get original scores by running with recording wrappers
        var recordingAgent = new RecordingAgentWrapper(agent);
        var recordingJudge = new RecordingJudgeModel(judgeModel);
        Map<String, EvalScore> originalScores = runEvaluation(
                recordingAgent, recordingJudge);

        // Second pass: replay from the loaded recording
        var replayAgent = new ReplayAgentWrapper(recording);
        var replayJudge = new ReplayJudgeModel(recording, judgeModel.modelId());
        Map<String, EvalScore> replayedScores = runEvaluation(
                replayAgent, replayJudge);

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, EvalScore> entry : originalScores.entrySet()) {
            String metricName = entry.getKey();
            EvalScore original = entry.getValue();
            EvalScore replayed = replayedScores.get(metricName);

            if (replayed == null) {
                mismatches.add(metricName + ": missing in replay");
            } else if (Double.compare(original.value(), replayed.value()) != 0) {
                mismatches.add(metricName + ": original="
                        + original.value() + " replayed=" + replayed.value());
            }
        }

        boolean allMatch = mismatches.isEmpty();
        LOG.info("Replay verification for '{}': {}",
                recordingName, allMatch ? "ALL MATCH" : mismatches.size() + " mismatch(es)");

        return new ReplayVerification(
                recordingName,
                originalScores,
                replayedScores,
                allMatch,
                mismatches
        );
    }

    private Map<String, EvalScore> runEvaluation(
            Function<String, String> agentFn,
            JudgeModel judge) {
        Map<String, EvalScore> scores = new LinkedHashMap<>();

        for (AgentTestCase testCase : testCases) {
            // Invoke the agent if no actual output is set
            if (testCase.getActualOutput() == null
                    || testCase.getActualOutput().isEmpty()) {
                String output = agentFn.apply(testCase.getInput());
                testCase.setActualOutput(output);
            }

            // Evaluate each metric
            for (EvalMetric metric : metrics) {
                EvalScore score = metric.evaluate(testCase);
                String key = testCase.getInput() + "::" + metric.name();
                scores.put(key, score);
            }
        }

        return scores;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Function<String, String> agent;
        private JudgeModel judgeModel;
        private final List<EvalMetric> metrics = new ArrayList<>();
        private final List<AgentTestCase> testCases = new ArrayList<>();
        private RecordingStore recordingStore;
        private String recordingName;

        private Builder() {}

        public Builder agent(Function<String, String> agent) {
            this.agent = agent;
            return this;
        }

        public Builder judgeModel(JudgeModel judgeModel) {
            this.judgeModel = judgeModel;
            return this;
        }

        public Builder metric(EvalMetric metric) {
            this.metrics.add(Objects.requireNonNull(metric,
                    "metric must not be null"));
            return this;
        }

        public Builder metrics(List<EvalMetric> metrics) {
            Objects.requireNonNull(metrics, "metrics must not be null");
            this.metrics.addAll(metrics);
            return this;
        }

        public Builder testCase(AgentTestCase testCase) {
            this.testCases.add(Objects.requireNonNull(testCase,
                    "testCase must not be null"));
            return this;
        }

        public Builder testCases(List<AgentTestCase> testCases) {
            Objects.requireNonNull(testCases, "testCases must not be null");
            this.testCases.addAll(testCases);
            return this;
        }

        public Builder recordingStore(RecordingStore recordingStore) {
            this.recordingStore = recordingStore;
            return this;
        }

        public Builder recordingName(String recordingName) {
            this.recordingName = recordingName;
            return this;
        }

        public ReplaySuite build() {
            return new ReplaySuite(this);
        }
    }
}
