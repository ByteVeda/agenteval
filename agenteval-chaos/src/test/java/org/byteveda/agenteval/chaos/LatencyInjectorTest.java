package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LatencyInjectorTest {

    @Test
    void rejectsNegativeAdditionalMs() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LatencyInjector(-1))
                .withMessageContaining("negative");
    }

    @Test
    void rejectsNullTestCase() {
        var injector = new LatencyInjector(100);
        assertThatNullPointerException().isThrownBy(() -> injector.inject(null));
    }

    @Test
    void addsAdditionalMsToEveryToolCallDuration() {
        var testCase = AgentTestCase.builder()
                .input("q")
                .toolCalls(List.of(
                        new ToolCall("a", Map.of(), "ok", 150),
                        new ToolCall("b", Map.of(), "ok", 50)))
                .build();

        var result = new LatencyInjector(100).inject(testCase);

        assertThat(result.getToolCalls()).extracting(ToolCall::durationMs)
                .containsExactly(250L, 150L);
    }

    @Test
    void preservesToolNameArgumentsAndResult() {
        var testCase = AgentTestCase.builder()
                .input("q")
                .toolCalls(List.of(new ToolCall("search",
                        Map.of("q", "java"), "result-text", 10)))
                .build();

        var result = new LatencyInjector(5).inject(testCase);

        var call = result.getToolCalls().get(0);
        assertThat(call.name()).isEqualTo("search");
        assertThat(call.arguments()).containsEntry("q", "java");
        assertThat(call.result()).isEqualTo("result-text");
        assertThat(call.durationMs()).isEqualTo(15L);
    }

    @Test
    void returnsOriginalTestCaseWhenNoToolCalls() {
        var testCase = AgentTestCase.builder().input("q").build();

        var result = new LatencyInjector(100).inject(testCase);

        assertThat(result).isSameAs(testCase);
    }

    @Test
    void zeroAdditionalMsIsValidAndIdentity() {
        var original = new ToolCall("a", Map.of(), "ok", 150);
        var testCase = AgentTestCase.builder().input("q")
                .toolCalls(List.of(original)).build();

        var result = new LatencyInjector(0).inject(testCase);

        assertThat(result.getToolCalls().get(0).durationMs()).isEqualTo(150L);
    }

    @Test
    void descriptionIncludesMillisecondAmount() {
        assertThat(new LatencyInjector(500).description()).contains("500");
    }

    @Test
    void getAdditionalMsExposesConfiguredValue() {
        assertThat(new LatencyInjector(42).getAdditionalMs()).isEqualTo(42L);
    }
}
