package org.byteveda.agenteval.metrics.llm;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("removal")
class PromptTemplateTest {

    @Test
    void shouldLoadExistingTemplate() {
        String template = PromptTemplate.load(
                "com/agenteval/metrics/prompts/answer-relevancy.txt");
        assertThat(template).contains("{{input}}").contains("{{actualOutput}}");
    }

    @Test
    void shouldThrowOnMissingTemplate() {
        assertThatThrownBy(() -> PromptTemplate.load("nonexistent.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldRenderVariables() {
        String template = "Input: {{input}}, Output: {{output}}";
        String result = PromptTemplate.render(template, Map.of(
                "input", "Hello",
                "output", "World"));
        assertThat(result).isEqualTo("Input: Hello, Output: World");
    }

    @Test
    void shouldLeaveUnresolvedVariables() {
        String template = "{{resolved}} and {{unresolved}}";
        String result = PromptTemplate.render(template, Map.of("resolved", "yes"));
        assertThat(result).isEqualTo("yes and {{unresolved}}");
    }

    @Test
    void shouldHandleEmptyVariables() {
        String template = "No {{variables}} here";
        String result = PromptTemplate.render(template, Map.of());
        assertThat(result).isEqualTo("No {{variables}} here");
    }

    @Test
    void shouldHandleSpecialCharactersInValues() {
        String template = "Value: {{val}}";
        String result = PromptTemplate.render(template, Map.of("val", "$100 (USD)"));
        assertThat(result).isEqualTo("Value: $100 (USD)");
    }

    @Test
    void shouldLoadAndRender() {
        String result = PromptTemplate.loadAndRender(
                "com/agenteval/metrics/prompts/answer-relevancy.txt",
                Map.of("input", "test question", "actualOutput", "test answer",
                        "strictMode", ""));
        assertThat(result).contains("test question").contains("test answer");
        assertThat(result).doesNotContain("{{input}}");
    }

    @Test
    void shouldRejectNullResourcePath() {
        assertThatThrownBy(() -> PromptTemplate.load(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullTemplate() {
        assertThatThrownBy(() -> PromptTemplate.render(null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullVariables() {
        assertThatThrownBy(() -> PromptTemplate.render("test", null))
                .isInstanceOf(NullPointerException.class);
    }
}
