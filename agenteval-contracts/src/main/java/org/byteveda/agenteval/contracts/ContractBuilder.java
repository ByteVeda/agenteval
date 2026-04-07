package org.byteveda.agenteval.contracts;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.ToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Fluent builder for creating {@link Contract} instances.
 *
 * <p>Deterministic checks are accumulated with AND semantics — all must pass.
 * If {@link #judgedBy(JudgeModel)} is called, an {@link LLMJudgedContract} is produced instead.</p>
 *
 * @see Contracts
 */
public final class ContractBuilder {

    private final String name;
    private final ContractType type;
    private String description = "";
    private ContractSeverity severity = ContractSeverity.ERROR;

    private final List<Predicate<AgentTestCase>> predicates = new ArrayList<>();
    private JudgeModel judge;
    private String promptResourcePath;
    private double passThreshold = 0.8;

    ContractBuilder(String name, ContractType type) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public ContractBuilder description(String description) {
        this.description = Objects.requireNonNull(description);
        return this;
    }

    public ContractBuilder severity(ContractSeverity severity) {
        this.severity = Objects.requireNonNull(severity);
        return this;
    }

    // --- Deterministic output checks ---

    /**
     * Output must contain the given substring.
     */
    public ContractBuilder outputContains(String substring) {
        Objects.requireNonNull(substring);
        predicates.add(tc -> {
            String output = tc.getActualOutput();
            return output != null && output.contains(substring);
        });
        return this;
    }

    /**
     * Output must not contain the given substring.
     */
    public ContractBuilder outputDoesNotContain(String substring) {
        Objects.requireNonNull(substring);
        predicates.add(tc -> {
            String output = tc.getActualOutput();
            return output == null || !output.contains(substring);
        });
        return this;
    }

    /**
     * Output must match the given regex.
     */
    public ContractBuilder outputMatches(String regex) {
        Pattern pattern = Pattern.compile(Objects.requireNonNull(regex));
        predicates.add(tc -> {
            String output = tc.getActualOutput();
            return output != null && pattern.matcher(output).find();
        });
        return this;
    }

    /**
     * Output must not match the given regex.
     */
    public ContractBuilder outputDoesNotMatchRegex(String regex) {
        Pattern pattern = Pattern.compile(Objects.requireNonNull(regex));
        predicates.add(tc -> {
            String output = tc.getActualOutput();
            return output == null || !pattern.matcher(output).find();
        });
        return this;
    }

    /**
     * Output must be valid JSON.
     */
    public ContractBuilder outputMatchesJson() {
        predicates.add(tc -> {
            String output = tc.getActualOutput();
            if (output == null || output.isBlank()) {
                return false;
            }
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.readTree(output.strip());
                return true;
            } catch (Exception e) {
                return false;
            }
        });
        return this;
    }

    /**
     * Output length must be at most {@code maxChars} characters.
     */
    public ContractBuilder outputLengthAtMost(int maxChars) {
        if (maxChars < 0) {
            throw new IllegalArgumentException("maxChars must be >= 0");
        }
        predicates.add(tc -> {
            String output = tc.getActualOutput();
            return output == null || output.length() <= maxChars;
        });
        return this;
    }

    /**
     * Output length must be at least {@code minChars} characters.
     */
    public ContractBuilder outputLengthAtLeast(int minChars) {
        if (minChars < 0) {
            throw new IllegalArgumentException("minChars must be >= 0");
        }
        predicates.add(tc -> {
            String output = tc.getActualOutput();
            return output != null && output.length() >= minChars;
        });
        return this;
    }

    /**
     * Output must satisfy the given predicate.
     */
    public ContractBuilder outputSatisfies(Predicate<String> predicate) {
        Objects.requireNonNull(predicate);
        predicates.add(tc -> {
            String output = tc.getActualOutput();
            return output != null && predicate.test(output);
        });
        return this;
    }

    // --- Deterministic tool call checks ---

    /**
     * The named tool must never be called.
     */
    public ContractBuilder toolNeverCalled(String toolName) {
        Objects.requireNonNull(toolName);
        predicates.add(tc -> tc.getToolCalls().stream()
                .noneMatch(t -> t.name().equals(toolName)));
        return this;
    }

    /**
     * The named tool must always be called (at least once).
     */
    public ContractBuilder toolAlwaysCalled(String toolName) {
        Objects.requireNonNull(toolName);
        predicates.add(tc -> tc.getToolCalls().stream()
                .anyMatch(t -> t.name().equals(toolName)));
        return this;
    }

    /**
     * Total tool calls must be at most {@code max}.
     */
    public ContractBuilder toolCallCountAtMost(int max) {
        predicates.add(tc -> tc.getToolCalls().size() <= max);
        return this;
    }

    /**
     * Total tool calls must be at least {@code min}.
     */
    public ContractBuilder toolCallCountAtLeast(int min) {
        predicates.add(tc -> tc.getToolCalls().size() >= min);
        return this;
    }

    /**
     * The tool {@code toolName} must never appear before {@code requiredPrior} in the
     * tool call sequence. If {@code toolName} is called, {@code requiredPrior} must
     * appear earlier in the list.
     */
    public ContractBuilder toolNeverCalledBefore(String toolName, String requiredPrior) {
        Objects.requireNonNull(toolName);
        Objects.requireNonNull(requiredPrior);
        predicates.add(tc -> {
            List<ToolCall> calls = tc.getToolCalls();
            int priorIndex = -1;
            for (int i = 0; i < calls.size(); i++) {
                if (calls.get(i).name().equals(requiredPrior) && priorIndex == -1) {
                    priorIndex = i;
                }
                if (calls.get(i).name().equals(toolName)) {
                    if (priorIndex == -1) {
                        return false; // toolName called before requiredPrior
                    }
                }
            }
            return true;
        });
        return this;
    }

    // --- Full test case predicate ---

    /**
     * The test case must satisfy the given predicate.
     */
    public ContractBuilder satisfies(Predicate<AgentTestCase> predicate) {
        Objects.requireNonNull(predicate);
        predicates.add(predicate);
        return this;
    }

    // --- LLM-judged ---

    /**
     * Makes this an LLM-judged contract using the default prompt template.
     */
    public ContractBuilder judgedBy(JudgeModel judge) {
        this.judge = Objects.requireNonNull(judge);
        return this;
    }

    /**
     * Makes this an LLM-judged contract using a custom prompt template resource.
     */
    public ContractBuilder judgedBy(JudgeModel judge, String promptResourcePath) {
        this.judge = Objects.requireNonNull(judge);
        this.promptResourcePath = Objects.requireNonNull(promptResourcePath);
        return this;
    }

    /**
     * Sets the pass threshold for LLM-judged contracts (default 0.8).
     */
    public ContractBuilder passThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be between 0.0 and 1.0");
        }
        this.passThreshold = threshold;
        return this;
    }

    /**
     * Builds the contract.
     *
     * @throws IllegalStateException if no checks or judge have been configured
     */
    public Contract build() {
        if (judge != null) {
            String path = promptResourcePath != null
                    ? promptResourcePath
                    : "com/agenteval/contracts/prompts/generic-contract.txt";
            return new LLMJudgedContract(name, description, severity, type,
                    judge, path, passThreshold);
        }
        if (predicates.isEmpty()) {
            throw new IllegalStateException(
                    "Contract must have at least one check or be LLM-judged");
        }
        Predicate<AgentTestCase> combined = predicates.stream()
                .reduce(Predicate::and)
                .orElseThrow();
        return new DeterministicContract(name, description, severity, type, combined);
    }
}
