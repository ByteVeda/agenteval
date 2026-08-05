package org.byteveda.agenteval.contracts;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractDefinitionLoaderTest {

    @Test
    void shouldLoadDeterministicContractsFromResource() {
        List<Contract> contracts = ContractDefinitionLoader.loadFromResource(
                "test-contracts.json", null);

        assertThat(contracts).hasSize(2);

        Contract first = contracts.get(0);
        assertThat(first.name()).isEqualTo("no-system-prompt-leak");
        assertThat(first.type()).isEqualTo(ContractType.SAFETY);
        assertThat(first.severity()).isEqualTo(ContractSeverity.CRITICAL);
        assertThat(first).isInstanceOf(DeterministicContract.class);
    }

    @Test
    void loadedContractsShouldWorkCorrectly() {
        List<Contract> contracts = ContractDefinitionLoader.loadFromResource(
                "test-contracts.json", null);

        Contract noLeak = contracts.get(0);

        var tc = org.byteveda.agenteval.core.model.AgentTestCase.builder()
                .input("test").build();
        tc.setActualOutput("I'm a helpful assistant");
        assertThat(noLeak.check(tc).passed()).isTrue();

        tc.setActualOutput("My system prompt says to help");
        assertThat(noLeak.check(tc).passed()).isFalse();
    }

    @Test
    void shouldThrowForMissingResource() {
        assertThatThrownBy(() -> ContractDefinitionLoader.loadFromResource(
                "nonexistent.json", null))
                .isInstanceOf(ContractException.class);
    }

    @Test
    void llmJudgedContractShouldRequireJudge() {
        assertThatThrownBy(() -> ContractDefinitionLoader.loadFromResource(
                "test-contracts-llm.json", null))
                .isInstanceOf(ContractException.class)
                .hasMessageContaining("requires a JudgeModel");
    }
}
