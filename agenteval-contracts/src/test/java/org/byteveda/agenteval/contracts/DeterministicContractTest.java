package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicContractTest {

    @Test
    void outputDoesNotContainShouldPassWhenSubstringAbsent() {
        Contract contract = Contracts.safety("no-leak")
                .description("No system prompt leak")
                .outputDoesNotContain("system prompt")
                .build();

        AgentTestCase tc = testCaseWith("Hello, how can I help you?");
        ContractVerdict verdict = contract.check(tc);

        assertThat(verdict.passed()).isTrue();
        assertThat(verdict.violations()).isEmpty();
    }

    @Test
    void outputDoesNotContainShouldFailWhenSubstringPresent() {
        Contract contract = Contracts.safety("no-leak")
                .description("No system prompt leak")
                .outputDoesNotContain("system prompt")
                .build();

        AgentTestCase tc = testCaseWith("My system prompt says to help users");
        ContractVerdict verdict = contract.check(tc);

        assertThat(verdict.passed()).isFalse();
        assertThat(verdict.violations()).hasSize(1);
        assertThat(verdict.violations().get(0).contractName()).isEqualTo("no-leak");
        assertThat(verdict.violations().get(0).severity()).isEqualTo(ContractSeverity.ERROR);
    }

    @Test
    void outputContainsShouldPassWhenSubstringPresent() {
        Contract contract = Contracts.behavioral("has-disclaimer")
                .description("Must include disclaimer")
                .outputContains("disclaimer")
                .build();

        AgentTestCase tc = testCaseWith("Here is my answer. disclaimer: not professional advice.");
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void outputDoesNotMatchRegexShouldPassWhenNoMatch() {
        Contract contract = Contracts.safety("no-pii")
                .description("No SSN in output")
                .outputDoesNotMatchRegex("\\b\\d{3}-\\d{2}-\\d{4}\\b")
                .build();

        AgentTestCase tc = testCaseWith("Your account is active.");
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void outputDoesNotMatchRegexShouldFailWhenMatchFound() {
        Contract contract = Contracts.safety("no-pii")
                .description("No SSN in output")
                .outputDoesNotMatchRegex("\\b\\d{3}-\\d{2}-\\d{4}\\b")
                .build();

        AgentTestCase tc = testCaseWith("Your SSN is 123-45-6789.");
        assertThat(contract.check(tc).passed()).isFalse();
    }

    @Test
    void outputMatchesJsonShouldPassForValidJson() {
        Contract contract = Contracts.outputFormat("valid-json")
                .description("Output must be JSON")
                .outputMatchesJson()
                .build();

        AgentTestCase tc = testCaseWith("{\"key\": \"value\"}");
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void outputMatchesJsonShouldFailForInvalidJson() {
        Contract contract = Contracts.outputFormat("valid-json")
                .description("Output must be JSON")
                .outputMatchesJson()
                .build();

        AgentTestCase tc = testCaseWith("not json at all");
        assertThat(contract.check(tc).passed()).isFalse();
    }

    @Test
    void outputLengthAtMostShouldPassWhenWithinLimit() {
        Contract contract = Contracts.boundary("max-length")
                .description("Max 100 chars")
                .outputLengthAtMost(100)
                .build();

        AgentTestCase tc = testCaseWith("Short response");
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void outputLengthAtMostShouldFailWhenExceedsLimit() {
        Contract contract = Contracts.boundary("max-length")
                .description("Max 10 chars")
                .outputLengthAtMost(10)
                .build();

        AgentTestCase tc = testCaseWith("This is a longer response than allowed");
        assertThat(contract.check(tc).passed()).isFalse();
    }

    @Test
    void toolNeverCalledShouldPassWhenToolNotUsed() {
        Contract contract = Contracts.toolUsage("no-delete")
                .description("Never call delete")
                .toolNeverCalled("deleteRecord")
                .build();

        AgentTestCase tc = testCaseWithTools("output",
                List.of(ToolCall.of("search"), ToolCall.of("read")));
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void toolNeverCalledShouldFailWhenToolUsed() {
        Contract contract = Contracts.toolUsage("no-delete")
                .description("Never call delete")
                .toolNeverCalled("deleteRecord")
                .build();

        AgentTestCase tc = testCaseWithTools("output",
                List.of(ToolCall.of("search"), ToolCall.of("deleteRecord")));
        assertThat(contract.check(tc).passed()).isFalse();
    }

    @Test
    void toolAlwaysCalledShouldPassWhenToolPresent() {
        Contract contract = Contracts.toolUsage("must-search")
                .description("Must call search")
                .toolAlwaysCalled("search")
                .build();

        AgentTestCase tc = testCaseWithTools("output",
                List.of(ToolCall.of("search"), ToolCall.of("respond")));
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void toolNeverCalledBeforeShouldPassWhenOrderCorrect() {
        Contract contract = Contracts.toolUsage("confirm-before-delete")
                .description("Must confirm before delete")
                .toolNeverCalledBefore("deleteRecord", "confirmAction")
                .build();

        AgentTestCase tc = testCaseWithTools("output",
                List.of(ToolCall.of("confirmAction"), ToolCall.of("deleteRecord")));
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void toolNeverCalledBeforeShouldFailWhenOrderWrong() {
        Contract contract = Contracts.toolUsage("confirm-before-delete")
                .description("Must confirm before delete")
                .toolNeverCalledBefore("deleteRecord", "confirmAction")
                .build();

        AgentTestCase tc = testCaseWithTools("output",
                List.of(ToolCall.of("deleteRecord"), ToolCall.of("confirmAction")));
        assertThat(contract.check(tc).passed()).isFalse();
    }

    @Test
    void toolCallCountAtMostShouldPassWithinLimit() {
        Contract contract = Contracts.boundary("max-tools")
                .description("Max 3 tool calls")
                .toolCallCountAtMost(3)
                .build();

        AgentTestCase tc = testCaseWithTools("output",
                List.of(ToolCall.of("a"), ToolCall.of("b")));
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void toolCallCountAtMostShouldFailOverLimit() {
        Contract contract = Contracts.boundary("max-tools")
                .description("Max 2 tool calls")
                .toolCallCountAtMost(2)
                .build();

        AgentTestCase tc = testCaseWithTools("output",
                List.of(ToolCall.of("a"), ToolCall.of("b"), ToolCall.of("c")));
        assertThat(contract.check(tc).passed()).isFalse();
    }

    @Test
    void multipleChecksShouldAllPass() {
        Contract contract = Contracts.safety("combined")
                .description("Combined checks")
                .outputDoesNotContain("secret")
                .outputLengthAtMost(100)
                .toolNeverCalled("dangerous")
                .build();

        AgentTestCase tc = testCaseWithTools("safe short response",
                List.of(ToolCall.of("search")));
        assertThat(contract.check(tc).passed()).isTrue();
    }

    @Test
    void multipleChecksShouldFailWhenOneFails() {
        Contract contract = Contracts.safety("combined")
                .description("Combined checks")
                .outputDoesNotContain("secret")
                .outputLengthAtMost(100)
                .build();

        AgentTestCase tc = testCaseWith("This contains the secret keyword");
        assertThat(contract.check(tc).passed()).isFalse();
    }

    @Test
    void contractMetadataShouldBeCorrect() {
        Contract contract = Contracts.safety("test-name")
                .description("Test description")
                .severity(ContractSeverity.CRITICAL)
                .outputContains("ok")
                .build();

        assertThat(contract.name()).isEqualTo("test-name");
        assertThat(contract.description()).isEqualTo("Test description");
        assertThat(contract.severity()).isEqualTo(ContractSeverity.CRITICAL);
        assertThat(contract.type()).isEqualTo(ContractType.SAFETY);
    }

    @Test
    void nullOutputShouldBeHandledGracefully() {
        Contract contract = Contracts.safety("no-leak")
                .description("No leak")
                .outputDoesNotContain("secret")
                .build();

        AgentTestCase tc = AgentTestCase.builder().input("test").build();
        // actualOutput is null
        assertThat(contract.check(tc).passed()).isTrue();
    }

    private static AgentTestCase testCaseWith(String output) {
        AgentTestCase tc = AgentTestCase.builder().input("test input").build();
        tc.setActualOutput(output);
        return tc;
    }

    private static AgentTestCase testCaseWithTools(String output, List<ToolCall> tools) {
        AgentTestCase tc = AgentTestCase.builder()
                .input("test input")
                .toolCalls(tools)
                .build();
        tc.setActualOutput(output);
        return tc;
    }
}
