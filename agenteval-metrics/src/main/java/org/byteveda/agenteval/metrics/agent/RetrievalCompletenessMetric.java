package org.byteveda.agenteval.metrics.agent;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.template.PromptTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Measures whether the retrieval context covers all expected context items.
 *
 * <p>Supports two match modes:</p>
 * <ul>
 *   <li>{@link MatchMode#EXACT}: Deterministic set-intersection on context strings</li>
 *   <li>{@link MatchMode#SEMANTIC}: Delegates to LLM judge for semantic comparison</li>
 * </ul>
 */
public final class RetrievalCompletenessMetric implements EvalMetric {

    private static final String NAME = "RetrievalCompleteness";
    private static final String PROMPT_PATH =
            "com/agenteval/metrics/prompts/retrieval-completeness.txt";
    private static final double DEFAULT_THRESHOLD = 0.8;

    private final JudgeModel judge;
    private final double threshold;
    private final MatchMode matchMode;

    public enum MatchMode {
        EXACT, SEMANTIC
    }

    public RetrievalCompletenessMetric(JudgeModel judge) {
        this(judge, DEFAULT_THRESHOLD, MatchMode.SEMANTIC);
    }

    public RetrievalCompletenessMetric(JudgeModel judge, double threshold) {
        this(judge, threshold, MatchMode.SEMANTIC);
    }

    public RetrievalCompletenessMetric(JudgeModel judge, double threshold, MatchMode matchMode) {
        this.judge = Objects.requireNonNull(judge, "judge must not be null");
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException(
                    "threshold must be between 0.0 and 1.0, got: " + threshold);
        }
        this.threshold = threshold;
        this.matchMode = Objects.requireNonNull(matchMode, "matchMode must not be null");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public EvalScore evaluate(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");
        validate(testCase);

        if (matchMode == MatchMode.EXACT) {
            return evaluateExact(testCase);
        }
        return evaluateSemantic(testCase);
    }

    private void validate(AgentTestCase testCase) {
        if (testCase.getContext().isEmpty()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty context (expected documents)");
        }
        if (testCase.getRetrievalContext().isEmpty()) {
            throw new IllegalArgumentException(
                    NAME + " requires non-empty retrievalContext (retrieved documents)");
        }
    }

    private EvalScore evaluateExact(AgentTestCase testCase) {
        List<String> expected = testCase.getContext();
        Set<String> retrieved = new HashSet<>(testCase.getRetrievalContext());

        int found = 0;
        for (String doc : expected) {
            if (retrieved.contains(doc)) {
                found++;
            }
        }

        double score = (double) found / expected.size();
        score = Math.min(1.0, Math.max(0.0, score));
        String reason = String.format("Exact match: %d / %d expected documents found",
                found, expected.size());
        return EvalScore.of(score, threshold, reason);
    }

    private EvalScore evaluateSemantic(AgentTestCase testCase) {
        Map<String, String> variables = new HashMap<>();
        variables.put("context", formatDocList(testCase.getContext()));
        variables.put("retrievalContext", formatDocList(testCase.getRetrievalContext()));

        String prompt = PromptTemplate.loadAndRender(PROMPT_PATH, variables);
        JudgeResponse response = judge.judge(prompt);
        return EvalScore.of(response.score(), threshold, response.reason());
    }

    private static String formatDocList(List<String> docs) {
        var sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(docs.get(i));
            if (i < docs.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
