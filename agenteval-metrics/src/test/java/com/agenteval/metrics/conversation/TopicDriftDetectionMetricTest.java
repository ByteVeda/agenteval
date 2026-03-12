package com.agenteval.metrics.conversation;

import com.agenteval.core.embedding.EmbeddingModel;
import com.agenteval.core.model.AgentTestCase;
import com.agenteval.core.model.ConversationTestCase;
import com.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopicDriftDetectionMetricTest {

    private EmbeddingModel embeddingModel;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.modelId()).thenReturn("test-model");
    }

    @Test
    void shouldScoreHighForOnTopicConversation() {
        var sameEmb = List.of(1.0, 0.0, 0.0);
        when(embeddingModel.embed(anyString())).thenReturn(sameEmb);

        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("Tell me about Java")
                                .actualOutput("Java is a programming language")
                                .build(),
                        AgentTestCase.builder()
                                .input("What about Java generics?")
                                .actualOutput("Generics allow type parameters")
                                .build()))
                .build();

        var metric = new TopicDriftDetectionMetric(embeddingModel, 0.6);
        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(1.0, within(0.001));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void shouldScoreLowForOffTopicConversation() {
        when(embeddingModel.embed("Tell me about Java"))
                .thenReturn(List.of(1.0, 0.0, 0.0));
        when(embeddingModel.embed("What about cooking? I like pizza"))
                .thenReturn(List.of(0.0, 1.0, 0.0));

        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("Tell me about Java")
                                .actualOutput("Java is a language")
                                .build(),
                        AgentTestCase.builder()
                                .input("What about cooking?")
                                .actualOutput("I like pizza")
                                .build()))
                .build();

        var metric = new TopicDriftDetectionMetric(embeddingModel, 0.6);
        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isCloseTo(0.0, within(0.001));
        assertThat(score.passed()).isFalse();
    }

    @Test
    void shouldReturnPerfectScoreForSingleTurn() {
        var testCase = ConversationTestCase.builder()
                .turns(List.of(
                        AgentTestCase.builder()
                                .input("Hello")
                                .actualOutput("Hi")
                                .build()))
                .build();

        var metric = new TopicDriftDetectionMetric(embeddingModel);
        EvalScore score = metric.evaluate(testCase);

        assertThat(score.value()).isEqualTo(1.0);
    }

    @Test
    void shouldReturnCorrectName() {
        var metric = new TopicDriftDetectionMetric(embeddingModel);
        assertThat(metric.name()).isEqualTo("TopicDriftDetection");
    }

    @Test
    void shouldRejectNullEmbeddingModel() {
        assertThatThrownBy(() -> new TopicDriftDetectionMetric(null))
                .isInstanceOf(NullPointerException.class);
    }
}
