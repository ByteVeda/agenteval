package com.agenteval.metrics.llm;

import com.agenteval.core.judge.JudgeModel;
import com.agenteval.core.judge.JudgeResponse;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.EvalScore;
import com.agenteval.core.model.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LLMJudgeMetricTest {

    private static final String TEST_PROMPT = "com/agenteval/metrics/prompts/answer-relevancy.txt";

    @Test
    void shouldCallJudgeAndReturnScore() {
        JudgeModel judge = mock(JudgeModel.class);
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.85, "Relevant answer", TokenUsage.of(100, 50)));

        var metric = new TestMetric(judge, 0.7, TEST_PROMPT);
        var testCase = AgentTestCase.builder()
                .input("What is Java?")
                .actualOutput("Java is a programming language.")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.85, within(0.001));
        assertThat(score.threshold()).isEqualTo(0.7);
        assertThat(score.passed()).isTrue();
        assertThat(score.reason()).isEqualTo("Relevant answer");
    }

    @Test
    void shouldFailWhenBelowThreshold() {
        JudgeModel judge = mock(JudgeModel.class);
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.3, "Not relevant", null));

        var metric = new TestMetric(judge, 0.7, TEST_PROMPT);
        var testCase = AgentTestCase.builder()
                .input("What is Java?")
                .actualOutput("The weather is nice today.")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldRejectNullTestCase() {
        JudgeModel judge = mock(JudgeModel.class);
        var metric = new TestMetric(judge, 0.7, TEST_PROMPT);

        assertThatThrownBy(() -> metric.evaluate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectEmptyInput() {
        JudgeModel judge = mock(JudgeModel.class);
        var metric = new TestMetric(judge, 0.7, TEST_PROMPT);

        var testCase = AgentTestCase.builder()
                .input("")
                .actualOutput("Some output")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("input");
    }

    @Test
    void shouldRejectEmptyActualOutput() {
        JudgeModel judge = mock(JudgeModel.class);
        var metric = new TestMetric(judge, 0.7, TEST_PROMPT);

        var testCase = AgentTestCase.builder()
                .input("Some input")
                .actualOutput("")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actualOutput");
    }

    @Test
    void shouldRejectNullJudge() {
        assertThatThrownBy(() -> new TestMetric(null, 0.7, TEST_PROMPT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectInvalidThreshold() {
        JudgeModel judge = mock(JudgeModel.class);
        assertThatThrownBy(() -> new TestMetric(judge, 1.5, TEST_PROMPT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Concrete test implementation of LLMJudgeMetric.
     */
    static class TestMetric extends LLMJudgeMetric {
        TestMetric(JudgeModel judge, double threshold, String promptPath) {
            super(judge, threshold, promptPath);
        }

        @Override
        public String name() {
            return "TestMetric";
        }

        @Override
        protected Map<String, String> buildTemplateVariables(AgentTestCase testCase) {
            return Map.of(
                    "input", testCase.getInput(),
                    "actualOutput", testCase.getActualOutput(),
                    "strictMode", "");
        }
    }
}
