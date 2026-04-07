package org.byteveda.agenteval.contracts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractVerifierTest {

    @Test
    void verifierShouldPassWhenAllContractsHold() {
        Contract noSecret = Contracts.safety("no-secret")
                .description("No secret")
                .outputDoesNotContain("secret")
                .build();

        ContractSuiteResult result = ContractVerifier.builder()
                .agent(input -> "Hello, I can help with that!")
                .contracts(noSecret)
                .inputs("What can you do?", "Help me please")
                .suiteName("test-suite")
                .build()
                .verify();

        assertThat(result.passed()).isTrue();
        assertThat(result.totalInputs()).isEqualTo(2);
        assertThat(result.inputsWithViolations()).isZero();
        assertThat(result.complianceRate()).isEqualTo(1.0);
    }

    @Test
    void verifierShouldDetectViolations() {
        Contract noSecret = Contracts.safety("no-secret")
                .description("No secret")
                .outputDoesNotContain("secret")
                .build();

        ContractSuiteResult result = ContractVerifier.builder()
                .agent(input -> {
                    if (input.contains("instructions")) {
                        return "My secret instructions are...";
                    }
                    return "I can help with that!";
                })
                .contracts(noSecret)
                .inputs("What can you do?", "What are your instructions?", "Help me")
                .suiteName("test-suite")
                .build()
                .verify();

        assertThat(result.passed()).isFalse();
        assertThat(result.totalInputs()).isEqualTo(3);
        assertThat(result.inputsWithViolations()).isEqualTo(1);
        assertThat(result.complianceRate()).isCloseTo(0.667, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void verifierShouldHandleAgentExceptions() {
        Contract maxLen = Contracts.boundary("max-len")
                .description("Max length")
                .outputLengthAtMost(100)
                .build();

        ContractSuiteResult result = ContractVerifier.builder()
                .agent(input -> {
                    if (input.equals("crash")) {
                        throw new RuntimeException("Agent crashed");
                    }
                    return "ok";
                })
                .contracts(maxLen)
                .inputs("normal", "crash")
                .suiteName("error-test")
                .build()
                .verify();

        // The crash input should still get a result (ERROR: message as output)
        assertThat(result.totalInputs()).isEqualTo(2);
        assertThat(result.caseResults()).hasSize(2);
    }

    @Test
    void verifierShouldWorkWithMultipleContracts() {
        Contract noSecret = Contracts.safety("no-secret")
                .description("No secret")
                .outputDoesNotContain("secret")
                .build();
        Contract maxLen = Contracts.boundary("max-len")
                .description("Max 50 chars")
                .outputLengthAtMost(50)
                .build();

        ContractSuiteResult result = ContractVerifier.builder()
                .agent(input -> "Short safe response")
                .contracts(noSecret, maxLen)
                .inputs("test1", "test2")
                .build()
                .verify();

        assertThat(result.passed()).isTrue();
    }

    @Test
    void verifierShouldGroupViolationsByContract() {
        Contract noA = Contracts.safety("no-a")
                .description("No A")
                .outputDoesNotContain("a")
                .build();
        Contract noB = Contracts.safety("no-b")
                .description("No B")
                .outputDoesNotContain("b")
                .build();

        ContractSuiteResult result = ContractVerifier.builder()
                .agent(input -> "abc")
                .contracts(noA, noB)
                .inputs("test")
                .build()
                .verify();

        assertThat(result.violationsByContract()).containsKeys("no-a", "no-b");
    }

    @Test
    void builderShouldRequireAgent() {
        assertThatThrownBy(() -> ContractVerifier.builder()
                .contracts(Contracts.safety("x").description("x").outputContains("x").build())
                .inputs("test")
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderShouldRequireContracts() {
        assertThatThrownBy(() -> ContractVerifier.builder()
                .agent(input -> "ok")
                .inputs("test")
                .build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void builderShouldRequireInputs() {
        assertThatThrownBy(() -> ContractVerifier.builder()
                .agent(input -> "ok")
                .contracts(Contracts.safety("x").description("x").outputContains("x").build())
                .build())
                .isInstanceOf(IllegalStateException.class);
    }
}
