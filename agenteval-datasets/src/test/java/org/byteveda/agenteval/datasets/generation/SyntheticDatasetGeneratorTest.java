package org.byteveda.agenteval.datasets.generation;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.datasets.EvalDataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SyntheticDatasetGeneratorTest {

    private JudgeModel judge;
    private SyntheticDatasetGenerator generator;

    @BeforeEach
    void setUp() {
        judge = mock(JudgeModel.class);
        generator = SyntheticDatasetGenerator.builder()
                .config(GenerationConfig.builder()
                        .judgeModel(judge)
                        .maxCasesPerDocument(3)
                        .build())
                .build();
    }

    @Test
    void shouldGenerateFromDocuments() {
        when(judge.judge(anyString())).thenReturn(new JudgeResponse(1.0,
                "Q: What is Java?\nA: A programming language\n"
                        + "Q: Who created Java?\nA: James Gosling", null));

        EvalDataset dataset = generator.fromDocuments(List.of("Java documentation"));

        assertThat(dataset.getTestCases()).hasSize(2);
        assertThat(dataset.getTestCases().get(0).getInput()).isEqualTo("What is Java?");
        assertThat(dataset.getTestCases().get(0).getExpectedOutput())
                .isEqualTo("A programming language");
        assertThat(dataset.getName()).isEqualTo("synthetic");
    }

    @Test
    void shouldGenerateVariations() {
        when(judge.judge(anyString())).thenReturn(new JudgeResponse(1.0,
                "Q: Explain Java\nA: It's a language\n"
                        + "Q: Describe Java briefly\nA: A compiled language", null));

        var source = AgentTestCase.builder()
                .input("What is Java?")
                .expectedOutput("A programming language")
                .build();

        EvalDataset dataset = generator.variations(List.of(source));

        assertThat(dataset.getTestCases()).hasSize(2);
        assertThat(dataset.getName()).isEqualTo("synthetic-variations");
    }

    @Test
    void shouldGenerateAdversarial() {
        when(judge.judge(anyString())).thenReturn(new JudgeResponse(1.0,
                "Q: Is Java a type of coffee?\n"
                        + "A: Java is both a coffee and a programming language", null));

        var source = AgentTestCase.builder()
                .input("What is Java?")
                .expectedOutput("A programming language")
                .build();

        EvalDataset dataset = generator.adversarial(List.of(source));

        assertThat(dataset.getTestCases()).hasSize(1);
        assertThat(dataset.getName()).isEqualTo("synthetic-adversarial");
    }

    @Test
    void shouldParseQAPairs() {
        String text = "Q: First question\nA: First answer\nQ: Second question\nA: Second answer";
        var pairs = SyntheticDatasetGenerator.parseQAPairs(text);

        assertThat(pairs).hasSize(2);
        assertThat(pairs.get(0).getInput()).isEqualTo("First question");
        assertThat(pairs.get(0).getExpectedOutput()).isEqualTo("First answer");
        assertThat(pairs.get(1).getInput()).isEqualTo("Second question");
    }

    @Test
    void shouldHandleEmptyText() {
        assertThat(SyntheticDatasetGenerator.parseQAPairs("")).isEmpty();
        assertThat(SyntheticDatasetGenerator.parseQAPairs(null)).isEmpty();
    }

    @Test
    void shouldHandleTextWithNoQAPairs() {
        assertThat(SyntheticDatasetGenerator.parseQAPairs("Just some random text")).isEmpty();
    }
}
