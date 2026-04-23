package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SchemaMutationInjectorTest {

    @Test
    void rejectsNullMutationType() {
        assertThatNullPointerException()
                .isThrownBy(() -> new SchemaMutationInjector(null))
                .withMessageContaining("mutationType");
    }

    @Test
    void defaultConstructorUsesWrapInEnvelope() {
        assertThat(new SchemaMutationInjector().getMutationType())
                .isEqualTo(SchemaMutationInjector.MutationType.WRAP_IN_ENVELOPE);
    }

    @Test
    void rejectsNullTestCase() {
        var injector = new SchemaMutationInjector();
        assertThatNullPointerException().isThrownBy(() -> injector.inject(null));
    }

    @Test
    void wrapInEnvelopeWrapsEveryToolResult() {
        var testCase = AgentTestCase.builder().input("q")
                .toolCalls(List.of(new ToolCall("a", Map.of(), "original", 0)))
                .build();

        var result = new SchemaMutationInjector(
                SchemaMutationInjector.MutationType.WRAP_IN_ENVELOPE).inject(testCase);

        String mutated = result.getToolCalls().get(0).result();
        assertThat(mutated).contains("\"status\": \"ok\"");
        assertThat(mutated).contains("\"payload\": \"original\"");
        assertThat(mutated).contains("\"version\": \"2.0\"");
    }

    @Test
    void nestInDataWrapsResultInsideDataField() {
        var testCase = AgentTestCase.builder().input("q")
                .toolCalls(List.of(new ToolCall("a", Map.of(), "payload", 0)))
                .build();

        var result = new SchemaMutationInjector(
                SchemaMutationInjector.MutationType.NEST_IN_DATA).inject(testCase);

        String mutated = result.getToolCalls().get(0).result();
        assertThat(mutated).contains("\"data\":");
        assertThat(mutated).contains("\"result\": \"payload\"");
        assertThat(mutated).contains("\"deprecated\": true");
    }

    @Test
    void truncateHalvesLongResultsAndMarksTruncated() {
        String longResult = "a".repeat(100);
        var testCase = AgentTestCase.builder().input("q")
                .toolCalls(List.of(new ToolCall("a", Map.of(), longResult, 0)))
                .build();

        var result = new SchemaMutationInjector(
                SchemaMutationInjector.MutationType.TRUNCATE).inject(testCase);

        String mutated = result.getToolCalls().get(0).result();
        assertThat(mutated).endsWith("[TRUNCATED]");
        assertThat(mutated.length()).isLessThan(longResult.length());
    }

    @Test
    void nullToolResultIsReplacedWithSchemaErrorDocument() {
        var testCase = AgentTestCase.builder().input("q")
                .toolCalls(List.of(new ToolCall("a", Map.of(), null, 0)))
                .build();

        var result = new SchemaMutationInjector().inject(testCase);

        assertThat(result.getToolCalls().get(0).result())
                .contains("unexpected_schema");
    }

    @Test
    void returnsOriginalTestCaseWhenNoToolCalls() {
        var testCase = AgentTestCase.builder().input("q").build();

        var result = new SchemaMutationInjector().inject(testCase);

        assertThat(result).isSameAs(testCase);
    }

    @Test
    void escapesEmbeddedQuotesInPayload() {
        var testCase = AgentTestCase.builder().input("q")
                .toolCalls(List.of(new ToolCall("a", Map.of(),
                        "has \"quotes\" inside", 0)))
                .build();

        var result = new SchemaMutationInjector().inject(testCase);

        assertThat(result.getToolCalls().get(0).result())
                .contains("\\\"quotes\\\"");
    }

    @Test
    void descriptionIncludesMutationType() {
        assertThat(new SchemaMutationInjector(
                SchemaMutationInjector.MutationType.TRUNCATE).description())
                .contains("TRUNCATE");
    }
}
