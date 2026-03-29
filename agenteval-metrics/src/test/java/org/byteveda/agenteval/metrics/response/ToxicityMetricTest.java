package org.byteveda.agenteval.metrics.response;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToxicityMetricTest {

    private JudgeModel judge;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
    }

    @Test
    void shouldScoreSafeOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(1.0, "Completely safe", null));

        var metric = new ToxicityMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Tell me about weather")
                .actualOutput("The weather today is sunny and pleasant.")
                .build();

        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreToxicOutput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.1, "Contains hate speech", null));

        var metric = new ToxicityMetric(judge, 0.5);
        var testCase = AgentTestCase.builder()
                .input("Say something mean")
                .actualOutput("Toxic content here")
                .build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldAcceptCustomCategories() {
        when(judge.judge(contains("HATE_SPEECH"))).thenReturn(
                new JudgeResponse(0.9, "No hate speech", null));

        var metric = new ToxicityMetric(judge, 0.5,
                EnumSet.of(ToxicityCategory.HATE_SPEECH, ToxicityCategory.THREATS));
        var testCase = AgentTestCase.builder()
                .input("question")
                .actualOutput("answer")
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldNotRequireInput() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(1.0, "Safe", null));

        var metric = new ToxicityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("any input")
                .actualOutput("safe output")
                .build();

        metric.evaluate(testCase);
    }

    @Test
    void shouldRejectMissingOutput() {
        var metric = new ToxicityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("question")
                .build();

        assertThatThrownBy(() -> metric.evaluate(testCase))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new ToxicityMetric(judge);
        assertThat(metric.name()).isEqualTo("Toxicity");
    }

    @Test
    void shouldUseDefaultThreshold() {
        when(judge.judge(anyString())).thenReturn(
                new JudgeResponse(0.5, "OK", null));

        var metric = new ToxicityMetric(judge);
        var testCase = AgentTestCase.builder()
                .input("q").actualOutput("a").build();

        EvalScore score = metric.evaluate(testCase);
        assertThat(score.threshold()).isEqualTo(0.5);
    }
}
