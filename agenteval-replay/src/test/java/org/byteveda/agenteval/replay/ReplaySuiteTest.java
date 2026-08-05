package org.byteveda.agenteval.replay;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.byteveda.agenteval.core.model.TokenUsage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReplaySuiteTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRecordAndPersist() {
        JudgeModel judge = mock(JudgeModel.class);
        when(judge.modelId()).thenReturn("test-model");
        when(judge.judge(any())).thenReturn(
                new JudgeResponse(0.9, "good", TokenUsage.of(5, 10)));

        EvalMetric metric = mock(EvalMetric.class);
        when(metric.name()).thenReturn("TestMetric");
        when(metric.evaluate(any())).thenReturn(
                EvalScore.of(0.9, 0.7, "good"));

        AgentTestCase testCase = AgentTestCase.builder()
                .input("What is Java?")
                .build();

        var store = new RecordingStore(tempDir);
        var suite = ReplaySuite.builder()
                .agent(input -> "Java is a programming language")
                .judgeModel(judge)
                .metric(metric)
                .testCase(testCase)
                .recordingStore(store)
                .recordingName("test-run")
                .build();

        Recording recording = suite.record();

        assertThat(recording.name()).isEqualTo("test-run");
        assertThat(recording.interactions()).isNotEmpty();
        assertThat(store.exists("test-run")).isTrue();
    }

    @Test
    void shouldRejectMissingAgent() {
        JudgeModel judge = mock(JudgeModel.class);
        EvalMetric metric = mock(EvalMetric.class);

        assertThatThrownBy(() -> ReplaySuite.builder()
                .judgeModel(judge)
                .metric(metric)
                .testCase(AgentTestCase.builder().input("x").build())
                .recordingStore(new RecordingStore(tempDir))
                .recordingName("test")
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectMissingMetrics() {
        JudgeModel judge = mock(JudgeModel.class);

        assertThatThrownBy(() -> ReplaySuite.builder()
                .agent(input -> "output")
                .judgeModel(judge)
                .testCase(AgentTestCase.builder().input("x").build())
                .recordingStore(new RecordingStore(tempDir))
                .recordingName("test")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metric");
    }

    @Test
    void shouldRejectMissingTestCases() {
        JudgeModel judge = mock(JudgeModel.class);
        EvalMetric metric = mock(EvalMetric.class);

        assertThatThrownBy(() -> ReplaySuite.builder()
                .agent(input -> "output")
                .judgeModel(judge)
                .metric(metric)
                .recordingStore(new RecordingStore(tempDir))
                .recordingName("test")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test case");
    }

    @Test
    void shouldRejectMissingRecordingName() {
        JudgeModel judge = mock(JudgeModel.class);
        EvalMetric metric = mock(EvalMetric.class);

        assertThatThrownBy(() -> ReplaySuite.builder()
                .agent(input -> "output")
                .judgeModel(judge)
                .metric(metric)
                .testCase(AgentTestCase.builder().input("x").build())
                .recordingStore(new RecordingStore(tempDir))
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void replayShouldThrowWhenRecordingNotFound() {
        JudgeModel judge = mock(JudgeModel.class);
        when(judge.modelId()).thenReturn("test-model");

        EvalMetric metric = mock(EvalMetric.class);
        when(metric.name()).thenReturn("TestMetric");

        var suite = ReplaySuite.builder()
                .agent(input -> "output")
                .judgeModel(judge)
                .metric(metric)
                .testCase(AgentTestCase.builder().input("x").build())
                .recordingStore(new RecordingStore(tempDir))
                .recordingName("nonexistent")
                .build();

        assertThatThrownBy(suite::replay)
                .isInstanceOf(ReplayMismatchException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void shouldRecordAgentInteraction() {
        JudgeModel judge = mock(JudgeModel.class);
        when(judge.modelId()).thenReturn("test-model");
        when(judge.judge(any())).thenReturn(
                new JudgeResponse(0.8, "ok", TokenUsage.of(5, 10)));

        EvalMetric metric = mock(EvalMetric.class);
        when(metric.name()).thenReturn("Metric1");
        when(metric.evaluate(any())).thenReturn(
                EvalScore.of(0.8, 0.7, "ok"));

        AgentTestCase testCase = AgentTestCase.builder()
                .input("hello")
                .build();

        var store = new RecordingStore(tempDir);
        var suite = ReplaySuite.builder()
                .agent(input -> "response to " + input)
                .judgeModel(judge)
                .metric(metric)
                .testCase(testCase)
                .recordingStore(store)
                .recordingName("agent-test")
                .build();

        Recording recording = suite.record();
        assertThat(recording.agentInteractions()).hasSize(1);
        assertThat(recording.agentInteractions().getFirst().input())
                .isEqualTo("hello");
        assertThat(recording.agentInteractions().getFirst().output())
                .isEqualTo("response to hello");
    }
}
