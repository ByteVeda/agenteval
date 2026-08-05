package org.byteveda.agenteval.contracts;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregate result of running a contract suite across all inputs.
 */
public record ContractSuiteResult(
        String suiteName,
        List<ContractCaseResult> caseResults,
        int totalInputs,
        int inputsWithViolations,
        long durationMs
) {
    public ContractSuiteResult {
        Objects.requireNonNull(suiteName, "suiteName must not be null");
        caseResults = caseResults == null ? List.of() : List.copyOf(caseResults);
    }

    /**
     * A contract suite passes only if zero violations across all inputs.
     */
    public boolean passed() {
        return inputsWithViolations == 0;
    }

    /**
     * Returns the compliance rate as a value between 0.0 and 1.0.
     */
    public double complianceRate() {
        if (totalInputs == 0) {
            return 1.0;
        }
        return (double) (totalInputs - inputsWithViolations) / totalInputs;
    }

    /**
     * Returns all violations across all inputs, flattened.
     */
    public List<ContractViolation> allViolations() {
        return caseResults.stream()
                .flatMap(cr -> cr.violations().stream())
                .toList();
    }

    /**
     * Returns violations grouped by contract name.
     */
    public Map<String, List<ContractViolation>> violationsByContract() {
        return allViolations().stream()
                .collect(Collectors.groupingBy(ContractViolation::contractName));
    }

    /**
     * Prints a summary report to stdout.
     */
    public void summary() {
        System.out.printf("=== Contract Suite: %s ===%n", suiteName);
        System.out.printf("Inputs: %d | Passed: %d | Violations: %d | Compliance: %.1f%%%n",
                totalInputs, totalInputs - inputsWithViolations,
                inputsWithViolations, complianceRate() * 100);
        System.out.printf("Duration: %dms%n", durationMs);

        List<ContractViolation> violations = allViolations();
        if (!violations.isEmpty()) {
            System.out.println();
            System.out.println("--- Violations ---");
            for (ContractViolation v : violations) {
                System.out.printf("[%s] %s%n  Evidence: %s%n",
                        v.severity(), v.contractName(), v.evidence());
            }
        }

        Map<String, List<ContractViolation>> byContract = violationsByContract();
        if (!byContract.isEmpty()) {
            System.out.println();
            System.out.println("--- Per-Contract Summary ---");
            byContract.forEach((name, vs) ->
                    System.out.printf("  %-40s %d/%d violations  [%s]%n",
                            name, vs.size(), totalInputs, vs.get(0).severity()));
        }
    }
}
