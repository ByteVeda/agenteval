package org.byteveda.agenteval.gradle;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.metrics.agent.ToolSelectionAccuracyMetric;
import org.byteveda.agenteval.metrics.response.AnswerRelevancyMetric;
import org.byteveda.agenteval.metrics.response.BiasMetric;
import org.byteveda.agenteval.metrics.response.CoherenceMetric;
import org.byteveda.agenteval.metrics.response.ConcisenessMetric;
import org.byteveda.agenteval.metrics.response.CorrectnessMetric;
import org.byteveda.agenteval.metrics.response.FaithfulnessMetric;
import org.byteveda.agenteval.metrics.response.HallucinationMetric;
import org.byteveda.agenteval.metrics.response.ToxicityMetric;
import org.byteveda.agenteval.metrics.rag.ContextualPrecisionMetric;
import org.byteveda.agenteval.metrics.rag.ContextualRecallMetric;
import org.byteveda.agenteval.metrics.rag.ContextualRelevancyMetric;
import org.byteveda.agenteval.metrics.agent.TaskCompletionMetric;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves metric name strings to {@link EvalMetric} instances.
 *
 * <p>Metric names are case-insensitive. All LLM-based metrics require a {@link JudgeModel}.</p>
 */
public final class MetricResolver {

    private static final Map<String, Function<JudgeModel, EvalMetric>> LLM_METRICS = Map.ofEntries(
            entry("answerrelevancy", AnswerRelevancyMetric::new),
            entry("faithfulness", FaithfulnessMetric::new),
            entry("correctness", CorrectnessMetric::new),
            entry("hallucination", HallucinationMetric::new),
            entry("toxicity", ToxicityMetric::new),
            entry("coherence", CoherenceMetric::new),
            entry("conciseness", ConcisenessMetric::new),
            entry("bias", BiasMetric::new),
            entry("contextualrelevancy", ContextualRelevancyMetric::new),
            entry("contextualprecision", ContextualPrecisionMetric::new),
            entry("contextualrecall", ContextualRecallMetric::new),
            entry("taskcompletion", TaskCompletionMetric::new)
    );

    private static final Map<String, EvalMetric> STANDALONE_METRICS = Map.of(
            "toolselectionaccuracy", new ToolSelectionAccuracyMetric()
    );

    private MetricResolver() {}

    /**
     * Resolves a metric by name. LLM-based metrics use the provided judge.
     *
     * @param name  the metric name (case-insensitive)
     * @param judge the judge model for LLM-based metrics (may be null for standalone metrics)
     * @return the resolved metric
     * @throws IllegalArgumentException if the metric name is unknown
     */
    public static EvalMetric resolve(String name, JudgeModel judge) {
        String key = normalize(name);

        EvalMetric standalone = STANDALONE_METRICS.get(key);
        if (standalone != null) {
            return standalone;
        }

        Function<JudgeModel, EvalMetric> factory = LLM_METRICS.get(key);
        if (factory != null) {
            if (judge == null) {
                throw new IllegalArgumentException(
                        "Metric '" + name + "' requires a judge model");
            }
            return factory.apply(judge);
        }

        throw new IllegalArgumentException("Unknown metric: " + name
                + ". Available: " + availableMetrics());
    }

    /**
     * Returns comma-separated list of all known metric names.
     */
    public static String availableMetrics() {
        var all = new java.util.TreeSet<String>();
        all.addAll(LLM_METRICS.keySet());
        all.addAll(STANDALONE_METRICS.keySet());
        return String.join(", ", all);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }

    private static <T> Map.Entry<String, T> entry(String key, T value) {
        return Map.entry(key, value);
    }
}
