package com.agenteval.gradle;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.metric.EvalMetric;
import com.agenteval.core.model.TokenUsage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricResolverTest {

    private final JudgeModel stubJudge = new JudgeModel() {
        @Override
        public JudgeResponse judge(String prompt) {
            return new JudgeResponse(0.8, "good", TokenUsage.of(10, 5));
        }

        @Override
        public String modelId() { return "stub"; }
    };

    @Test
    void shouldResolveLlmMetricByName() {
        EvalMetric metric = MetricResolver.resolve("AnswerRelevancy", stubJudge);

        assertThat(metric).isNotNull();
        assertThat(metric.name()).isEqualTo("AnswerRelevancy");
    }

    @Test
    void shouldResolveCaseInsensitively() {
        EvalMetric metric = MetricResolver.resolve("answerrelevancy", stubJudge);

        assertThat(metric).isNotNull();
        assertThat(metric.name()).isEqualTo("AnswerRelevancy");
    }

    @Test
    void shouldResolveWithHyphensAndUnderscores() {
        EvalMetric metric = MetricResolver.resolve("answer-relevancy", stubJudge);
        assertThat(metric.name()).isEqualTo("AnswerRelevancy");

        EvalMetric metric2 = MetricResolver.resolve("answer_relevancy", stubJudge);
        assertThat(metric2.name()).isEqualTo("AnswerRelevancy");
    }

    @Test
    void shouldResolveStandaloneMetricWithoutJudge() {
        EvalMetric metric = MetricResolver.resolve("ToolSelectionAccuracy", null);

        assertThat(metric).isNotNull();
        assertThat(metric.name()).isEqualTo("ToolSelectionAccuracy");
    }

    @Test
    void shouldThrowForUnknownMetric() {
        assertThatThrownBy(() -> MetricResolver.resolve("NonexistentMetric", stubJudge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown metric");
    }

    @Test
    void shouldThrowWhenLlmMetricHasNoJudge() {
        assertThatThrownBy(() -> MetricResolver.resolve("Faithfulness", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a judge model");
    }

    @Test
    void shouldResolveAllKnownLlmMetrics() {
        String[] metricNames = {
            "Faithfulness", "Correctness", "Hallucination",
            "Toxicity", "Coherence", "Conciseness", "Bias",
            "ContextualRelevancy", "ContextualPrecision", "ContextualRecall",
            "TaskCompletion"
        };
        for (String name : metricNames) {
            EvalMetric metric = MetricResolver.resolve(name, stubJudge);
            assertThat(metric).as("Metric: " + name).isNotNull();
        }
    }

    @Test
    void availableMetricsShouldReturnNonEmptyString() {
        String available = MetricResolver.availableMetrics();

        assertThat(available).contains("answerrelevancy");
        assertThat(available).contains("faithfulness");
    }
}
