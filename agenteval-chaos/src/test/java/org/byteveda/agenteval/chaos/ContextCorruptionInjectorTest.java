package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextCorruptionInjectorTest {

    @Test
    void shouldRemoveAllContextWhenMissing() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("Summarize the documents")
                .retrievalContext(List.of(
                        "Document A: Revenue grew 15%",
                        "Document B: Costs decreased 5%"))
                .build();

        ContextCorruptionInjector injector = new ContextCorruptionInjector(
                ContextCorruptionInjector.CorruptionMode.MISSING);
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getRetrievalContext()).isEmpty();
    }

    @Test
    void shouldAddContradictoryEntries() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("What is the refund policy?")
                .retrievalContext(List.of(
                        "Refunds are available within 30 days"))
                .build();

        ContextCorruptionInjector injector = new ContextCorruptionInjector(
                ContextCorruptionInjector.CorruptionMode.CONTRADICTORY);
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getRetrievalContext()).hasSize(2);
        assertThat(result.getRetrievalContext().get(0))
                .isEqualTo("Refunds are available within 30 days");
        assertThat(result.getRetrievalContext().get(1))
                .contains("CONTRADICTORY");
    }

    @Test
    void shouldShuffleContextEntries() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("Follow the instructions")
                .retrievalContext(List.of(
                        "Step 1: Open the file",
                        "Step 2: Edit the content",
                        "Step 3: Save the file",
                        "Step 4: Close the editor"))
                .build();

        // Use a fixed seed so the test is deterministic
        ContextCorruptionInjector injector = new ContextCorruptionInjector(
                ContextCorruptionInjector.CorruptionMode.SHUFFLED, 42L);
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getRetrievalContext()).hasSize(4);
        assertThat(result.getRetrievalContext())
                .containsExactlyInAnyOrderElementsOf(
                        testCase.getRetrievalContext());
    }

    @Test
    void shouldHandleEmptyContextGracefully() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("No context provided")
                .build();

        ContextCorruptionInjector injector = new ContextCorruptionInjector(
                ContextCorruptionInjector.CorruptionMode.MISSING);
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getRetrievalContext()).isEmpty();
    }

    @Test
    void shouldHandleSingleEntryShuffleGracefully() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("Single context")
                .retrievalContext(List.of("Only entry"))
                .build();

        ContextCorruptionInjector injector = new ContextCorruptionInjector(
                ContextCorruptionInjector.CorruptionMode.SHUFFLED);
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getRetrievalContext())
                .containsExactly("Only entry");
    }

    @Test
    void shouldPreserveInputWhenCorruptingContext() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("Summarize the docs")
                .retrievalContext(List.of("Doc content"))
                .build();

        ContextCorruptionInjector injector = new ContextCorruptionInjector(
                ContextCorruptionInjector.CorruptionMode.MISSING);
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getInput()).isEqualTo("Summarize the docs");
    }

    @Test
    void shouldExposeCorruptionMode() {
        ContextCorruptionInjector injector = new ContextCorruptionInjector(
                ContextCorruptionInjector.CorruptionMode.CONTRADICTORY);
        assertThat(injector.getMode())
                .isEqualTo(
                        ContextCorruptionInjector.CorruptionMode.CONTRADICTORY);
    }

    @Test
    void shouldProvideDescription() {
        ContextCorruptionInjector injector = new ContextCorruptionInjector(
                ContextCorruptionInjector.CorruptionMode.SHUFFLED);
        assertThat(injector.description()).contains("SHUFFLED");
    }
}
