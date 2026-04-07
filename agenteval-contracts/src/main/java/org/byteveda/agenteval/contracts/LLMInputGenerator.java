package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.template.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates diverse test inputs using an LLM, informed by contract definitions.
 */
final class LLMInputGenerator implements InputGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(LLMInputGenerator.class);
    private static final String PROMPT_PATH =
            "com/agenteval/contracts/prompts/generate-contract-inputs.txt";
    private static final Pattern INPUT_PATTERN = Pattern.compile("^INPUT:\\s*(.+)$",
            Pattern.MULTILINE);

    private final JudgeModel judge;
    private final int inputsPerContract;

    LLMInputGenerator(JudgeModel judge, int inputsPerContract) {
        this.judge = Objects.requireNonNull(judge, "judge must not be null");
        if (inputsPerContract < 1) {
            throw new IllegalArgumentException("inputsPerContract must be >= 1");
        }
        this.inputsPerContract = inputsPerContract;
    }

    @Override
    public List<AgentTestCase> generate(List<Contract> contracts) {
        List<AgentTestCase> allInputs = new ArrayList<>();

        for (Contract contract : contracts) {
            try {
                Map<String, String> vars = Map.of(
                        "contractName", contract.name(),
                        "contractDescription", contract.description(),
                        "contractType", contract.type().name(),
                        "count", String.valueOf(inputsPerContract)
                );
                String prompt = PromptTemplate.loadAndRender(PROMPT_PATH, vars);
                JudgeResponse response = judge.judge(prompt);

                List<AgentTestCase> cases = parseInputs(response.reason());
                allInputs.addAll(cases);

                LOG.debug("Generated {} inputs for contract '{}'",
                        cases.size(), contract.name());
            } catch (Exception e) {
                LOG.warn("Failed to generate inputs for contract '{}': {}",
                        contract.name(), e.getMessage());
            }
        }

        return allInputs;
    }

    private static List<AgentTestCase> parseInputs(String text) {
        List<AgentTestCase> cases = new ArrayList<>();
        Matcher matcher = INPUT_PATTERN.matcher(text);
        while (matcher.find()) {
            String input = matcher.group(1).strip();
            if (!input.isEmpty()) {
                cases.add(AgentTestCase.builder().input(input).build());
            }
        }
        return cases;
    }
}
