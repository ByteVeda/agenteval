package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeContractTest {

    @Test
    void suiteShouldPassWhenAllChildrenPass() {
        Contract c1 = Contracts.safety("no-secret")
                .description("No secret")
                .outputDoesNotContain("secret")
                .build();
        Contract c2 = Contracts.boundary("max-len")
                .description("Max length")
                .outputLengthAtMost(100)
                .build();

        CompositeContract suite = Contracts.suite("safety-suite", c1, c2);
        AgentTestCase tc = testCaseWith("safe response");

        ContractVerdict verdict = suite.check(tc);
        assertThat(verdict.passed()).isTrue();
    }

    @Test
    void suiteShouldFailWhenAnyChildFails() {
        Contract c1 = Contracts.safety("no-secret")
                .description("No secret")
                .outputDoesNotContain("secret")
                .build();
        Contract c2 = Contracts.boundary("max-len")
                .description("Max length")
                .outputLengthAtMost(5)
                .build();

        CompositeContract suite = Contracts.suite("mixed-suite", c1, c2);
        AgentTestCase tc = testCaseWith("this is too long");

        ContractVerdict verdict = suite.check(tc);
        assertThat(verdict.passed()).isFalse();
        assertThat(verdict.violations()).hasSize(1);
    }

    @Test
    void suiteShouldCollectAllViolations() {
        Contract c1 = Contracts.safety("no-secret")
                .description("No secret")
                .outputDoesNotContain("secret")
                .build();
        Contract c2 = Contracts.boundary("max-len")
                .description("Max length")
                .outputLengthAtMost(5)
                .build();

        CompositeContract suite = Contracts.suite("both-fail", c1, c2);
        AgentTestCase tc = testCaseWith("this secret is too long");

        ContractVerdict verdict = suite.check(tc);
        assertThat(verdict.passed()).isFalse();
        assertThat(verdict.violations()).hasSize(2);
    }

    @Test
    void suiteShouldStopOnCriticalViolation() {
        Contract critical = Contracts.safety("critical-check")
                .description("Critical")
                .severity(ContractSeverity.CRITICAL)
                .outputDoesNotContain("danger")
                .build();
        Contract normal = Contracts.boundary("normal-check")
                .description("Normal")
                .outputLengthAtMost(5)
                .build();

        CompositeContract suite = Contracts.suite("stop-early", critical, normal);
        AgentTestCase tc = testCaseWith("danger zone and long");

        ContractVerdict verdict = suite.check(tc);
        assertThat(verdict.passed()).isFalse();
        // Should have stopped after the critical violation
        assertThat(verdict.violations()).hasSize(1);
        assertThat(verdict.violations().get(0).contractName()).isEqualTo("critical-check");
    }

    @Test
    void suiteShouldExposeChildren() {
        Contract c1 = Contracts.safety("a").description("A").outputContains("a").build();
        Contract c2 = Contracts.safety("b").description("B").outputContains("b").build();

        CompositeContract suite = Contracts.suite("test", c1, c2);
        assertThat(suite.contracts()).hasSize(2);
    }

    @Test
    void emptySuiteShouldThrow() {
        assertThatThrownBy(() -> new CompositeContract("empty", "desc",
                ContractSeverity.ERROR, ContractType.SAFETY, java.util.List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static AgentTestCase testCaseWith(String output) {
        AgentTestCase tc = AgentTestCase.builder().input("test input").build();
        tc.setActualOutput(output);
        return tc;
    }
}
