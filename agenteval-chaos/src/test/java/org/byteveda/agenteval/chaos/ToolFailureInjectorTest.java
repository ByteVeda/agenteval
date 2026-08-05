package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolFailureInjectorTest {

    @Test
    void shouldReplaceToolResultsWithErrorMessage() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("Look up weather")
                .toolCalls(List.of(
                        new ToolCall("weather_api", Map.of("city", "NYC"),
                                "Sunny, 72F", 150),
                        new ToolCall("forecast_api", Map.of("city", "NYC"),
                                "Rain expected tomorrow", 200)))
                .build();

        ToolFailureInjector injector =
                new ToolFailureInjector("ERROR: Tool unavailable");
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getToolCalls()).hasSize(2);
        assertThat(result.getToolCalls().get(0).result())
                .isEqualTo("ERROR: Tool unavailable");
        assertThat(result.getToolCalls().get(1).result())
                .isEqualTo("ERROR: Tool unavailable");
    }

    @Test
    void shouldPreserveToolNameAndArguments() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("Search")
                .toolCalls(List.of(
                        new ToolCall("search", Map.of("q", "test"),
                                "Found 5 results", 100)))
                .build();

        ToolFailureInjector injector =
                new ToolFailureInjector("ERROR: Connection timeout");
        AgentTestCase result = injector.inject(testCase);

        ToolCall modified = result.getToolCalls().getFirst();
        assertThat(modified.name()).isEqualTo("search");
        assertThat(modified.arguments()).containsEntry("q", "test");
        assertThat(modified.durationMs()).isEqualTo(100);
        assertThat(modified.result())
                .isEqualTo("ERROR: Connection timeout");
    }

    @Test
    void shouldReturnSameTestCaseWhenNoToolCalls() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("Simple question")
                .build();

        ToolFailureInjector injector = new ToolFailureInjector();
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getToolCalls()).isEmpty();
        assertThat(result.getInput()).isEqualTo("Simple question");
    }

    @Test
    void shouldUseDefaultErrorMessage() {
        ToolFailureInjector injector = new ToolFailureInjector();
        assertThat(injector.description())
                .contains("ERROR: Tool unavailable");
    }

    @Test
    void shouldPreserveInputAndExpectedOutput() {
        AgentTestCase testCase = AgentTestCase.builder()
                .input("What is the weather?")
                .expectedOutput("It is sunny")
                .toolCalls(List.of(
                        new ToolCall("weather", Map.of(), "Sunny", 50)))
                .build();

        ToolFailureInjector injector =
                new ToolFailureInjector("ERROR: Service down");
        AgentTestCase result = injector.inject(testCase);

        assertThat(result.getInput()).isEqualTo("What is the weather?");
        assertThat(result.getExpectedOutput()).isEqualTo("It is sunny");
    }

    @Test
    void shouldProvideDefaultErrorMessages() {
        List<String> defaults = ToolFailureInjector.defaultErrorMessages();
        assertThat(defaults).isNotEmpty();
        assertThat(defaults).contains(
                "ERROR: Tool unavailable",
                "ERROR: Connection timeout");
    }
}
