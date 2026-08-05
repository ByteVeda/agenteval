package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.template.PromptTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * A contract verified by an LLM judge for semantic checks that require reasoning.
 *
 * <p>Examples: "agent must always cite sources", "agent must never provide medical advice".</p>
 */
public non-sealed class LLMJudgedContract implements Contract {

    private final String name;
    private final String description;
    private final ContractSeverity severity;
    private final ContractType type;
    private final JudgeModel judge;
    private final String promptResourcePath;
    private final double passThreshold;

    LLMJudgedContract(String name, String description,
            ContractSeverity severity, ContractType type,
            JudgeModel judge, String promptResourcePath,
            double passThreshold) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.severity = Objects.requireNonNull(severity);
        this.type = Objects.requireNonNull(type);
        this.judge = Objects.requireNonNull(judge);
        this.promptResourcePath = Objects.requireNonNull(promptResourcePath);
        this.passThreshold = passThreshold;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public ContractSeverity severity() {
        return severity;
    }

    @Override
    public ContractType type() {
        return type;
    }

    @Override
    public ContractVerdict check(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");

        Map<String, String> variables = Map.of(
                "input", safe(testCase.getInput()),
                "output", safe(testCase.getActualOutput()),
                "contractName", name,
                "contractDescription", description
        );

        String prompt = PromptTemplate.loadAndRender(promptResourcePath, variables);
        JudgeResponse response = judge.judge(prompt);

        if (response.score() >= passThreshold) {
            return ContractVerdict.pass(name);
        }
        return ContractVerdict.violation(name, response.reason(), severity);
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
