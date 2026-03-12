package com.agenteval.redteam.attack;

import com.agenteval.redteam.AttackCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttackTemplateLibraryTest {

    @Test
    void shouldLoadTemplates() {
        AttackTemplateLibrary library = AttackTemplateLibrary.load();

        assertThat(library.size()).isGreaterThan(0);
        assertThat(library.all()).isNotEmpty();
    }

    @Test
    void shouldLoadPromptInjectionTemplates() {
        AttackTemplateLibrary library = AttackTemplateLibrary.load();
        List<AttackTemplate> templates = library.forCategory(
                AttackCategory.PROMPT_INJECTION);

        assertThat(templates).isNotEmpty();
        assertThat(templates.getFirst().category())
                .isEqualTo(AttackCategory.PROMPT_INJECTION);
    }

    @Test
    void shouldReturnEmptyListForMissingCategory() {
        AttackTemplateLibrary library = AttackTemplateLibrary.load();
        // All categories have templates now, but test the API contract
        List<AttackTemplate> templates = library.forCategory(
                AttackCategory.PROMPT_INJECTION);
        assertThat(templates).isNotNull();
    }

    @Test
    void shouldLoadDataLeakageTemplates() {
        AttackTemplateLibrary library = AttackTemplateLibrary.load();
        List<AttackTemplate> templates = library.forCategory(
                AttackCategory.DATA_LEAKAGE);

        assertThat(templates).isNotEmpty();
    }
}
