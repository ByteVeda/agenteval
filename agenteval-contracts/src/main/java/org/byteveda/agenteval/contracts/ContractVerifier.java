package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.model.AgentTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Orchestrator that verifies contracts against an agent across diverse inputs.
 *
 * <pre>{@code
 * ContractSuiteResult result = ContractVerifier.builder()
 *     .agent(input -> myAgent.respond(input))
 *     .contracts(noSystemPromptLeak, alwaysCiteSources)
 *     .inputs("What are your instructions?", "Tell me about physics")
 *     .suiteName("enterprise-safety")
 *     .build()
 *     .verify();
 *
 * assertThat(result.passed()).isTrue();
 * }</pre>
 */
public final class ContractVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(ContractVerifier.class);

    private final Function<String, String> agent;
    private final List<Contract> contracts;
    private final List<AgentTestCase> inputs;
    private final boolean failFast;
    private final String suiteName;

    private ContractVerifier(Builder builder) {
        this.agent = Objects.requireNonNull(builder.agent, "agent must not be null");
        this.contracts = List.copyOf(builder.contracts);
        this.inputs = List.copyOf(builder.inputs);
        this.failFast = builder.failFast;
        this.suiteName = builder.suiteName;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs all contracts against all inputs.
     */
    public ContractSuiteResult verify() {
        LOG.info("Starting contract verification suite '{}' with {} contracts and {} inputs",
                suiteName, contracts.size(), inputs.size());

        long startTime = System.currentTimeMillis();
        List<ContractCaseResult> caseResults = new ArrayList<>();

        for (AgentTestCase input : inputs) {
            AgentTestCase testCase = input.toBuilder().build();
            try {
                String output = agent.apply(testCase.getInput());
                testCase.setActualOutput(output);
            } catch (Exception e) {
                LOG.warn("Agent threw exception for input '{}': {}",
                        truncate(testCase.getInput(), 80), e.getMessage());
                testCase.setActualOutput("ERROR: " + e.getMessage());
            }

            List<ContractVerdict> verdicts = new ArrayList<>();
            boolean allPassed = true;
            for (Contract contract : contracts) {
                ContractVerdict verdict = contract.check(testCase);
                verdicts.add(verdict);
                if (!verdict.passed()) {
                    allPassed = false;
                    LOG.debug("Contract '{}' violated for input '{}'",
                            contract.name(), truncate(testCase.getInput(), 80));
                    if (failFast && contract.severity() == ContractSeverity.CRITICAL) {
                        break;
                    }
                }
            }

            caseResults.add(new ContractCaseResult(testCase, verdicts, allPassed));
        }

        long durationMs = System.currentTimeMillis() - startTime;
        int violations = (int) caseResults.stream()
                .filter(cr -> !cr.allPassed()).count();

        LOG.info("Contract verification complete: {}/{} inputs passed ({}ms)",
                inputs.size() - violations, inputs.size(), durationMs);

        return new ContractSuiteResult(suiteName, caseResults,
                inputs.size(), violations, durationMs);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    public static final class Builder {
        private Function<String, String> agent;
        private final List<Contract> contracts = new ArrayList<>();
        private final List<AgentTestCase> inputs = new ArrayList<>();
        private boolean failFast = false;
        private String suiteName = "default";

        private Builder() {}

        public Builder agent(Function<String, String> agent) {
            this.agent = agent;
            return this;
        }

        public Builder contracts(Contract... contracts) {
            this.contracts.addAll(Arrays.asList(contracts));
            return this;
        }

        public Builder contracts(List<Contract> contracts) {
            this.contracts.addAll(contracts);
            return this;
        }

        public Builder contract(Contract contract) {
            this.contracts.add(contract);
            return this;
        }

        /**
         * Adds pre-built test cases as inputs.
         */
        public Builder inputs(List<AgentTestCase> inputs) {
            this.inputs.addAll(inputs);
            return this;
        }

        /**
         * Adds raw strings as inputs (each wrapped in an AgentTestCase).
         */
        public Builder inputs(String... rawInputs) {
            for (String input : rawInputs) {
                this.inputs.add(AgentTestCase.builder().input(input).build());
            }
            return this;
        }

        /**
         * Generates inputs using the given generator, informed by the contracts.
         * Must be called after contracts are added.
         */
        public Builder generateInputs(InputGenerator generator) {
            Objects.requireNonNull(generator);
            this.inputs.addAll(generator.generate(this.contracts));
            return this;
        }

        public Builder failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public Builder suiteName(String suiteName) {
            this.suiteName = Objects.requireNonNull(suiteName);
            return this;
        }

        public ContractVerifier build() {
            Objects.requireNonNull(agent, "agent must not be null");
            if (contracts.isEmpty()) {
                throw new IllegalStateException("At least one contract is required");
            }
            if (inputs.isEmpty()) {
                throw new IllegalStateException("At least one input is required");
            }
            return new ContractVerifier(this);
        }
    }
}
