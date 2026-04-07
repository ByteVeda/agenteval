package org.byteveda.agenteval.chaos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Provides built-in chaos engineering scenarios for each {@link ChaosCategory}.
 *
 * <p>Scenarios are hardcoded (not loaded from JSON) because they include
 * injector instances that cannot be serialized.</p>
 */
public final class ChaosScenarioLibrary {

    private static final Map<ChaosCategory, List<ChaosScenario>> SCENARIOS;

    static {
        SCENARIOS = new EnumMap<>(ChaosCategory.class);

        // Tool failure scenarios
        List<ChaosScenario> toolFailures = new ArrayList<>();
        toolFailures.add(new ChaosScenario(
                "tool-unavailable",
                ChaosCategory.TOOL_FAILURE,
                "All tools return unavailable errors",
                "Look up the weather forecast for New York City",
                new ToolFailureInjector("ERROR: Tool unavailable")));
        toolFailures.add(new ChaosScenario(
                "tool-timeout",
                ChaosCategory.TOOL_FAILURE,
                "All tools return timeout errors",
                "Search the database for customer order #12345",
                new ToolFailureInjector("ERROR: Connection timeout")));
        toolFailures.add(new ChaosScenario(
                "tool-server-error",
                ChaosCategory.TOOL_FAILURE,
                "All tools return 500 server errors",
                "Calculate the total revenue for Q3",
                new ToolFailureInjector(
                        "ERROR: Service returned 500 Internal Server Error")));
        toolFailures.add(new ChaosScenario(
                "tool-auth-failure",
                ChaosCategory.TOOL_FAILURE,
                "All tools return authentication errors",
                "Retrieve the user profile for user@example.com",
                new ToolFailureInjector("ERROR: Authentication failed")));
        SCENARIOS.put(ChaosCategory.TOOL_FAILURE, List.copyOf(toolFailures));

        // Context corruption scenarios
        List<ChaosScenario> contextCorruptions = new ArrayList<>();
        contextCorruptions.add(new ChaosScenario(
                "context-missing",
                ChaosCategory.CONTEXT_CORRUPTION,
                "All retrieval context is removed",
                "Based on the provided documents, summarize the key findings",
                new ContextCorruptionInjector(
                        ContextCorruptionInjector.CorruptionMode.MISSING)));
        contextCorruptions.add(new ChaosScenario(
                "context-contradictory",
                ChaosCategory.CONTEXT_CORRUPTION,
                "Contradictory information is injected into context",
                "What does the policy document say about refund eligibility?",
                new ContextCorruptionInjector(
                        ContextCorruptionInjector.CorruptionMode.CONTRADICTORY)));
        contextCorruptions.add(new ChaosScenario(
                "context-shuffled",
                ChaosCategory.CONTEXT_CORRUPTION,
                "Context entries are shuffled out of order",
                "Follow the step-by-step instructions from the manual",
                new ContextCorruptionInjector(
                        ContextCorruptionInjector.CorruptionMode.SHUFFLED)));
        SCENARIOS.put(ChaosCategory.CONTEXT_CORRUPTION,
                List.copyOf(contextCorruptions));

        // Latency scenarios
        List<ChaosScenario> latencyScenarios = new ArrayList<>();
        latencyScenarios.add(new ChaosScenario(
                "high-latency",
                ChaosCategory.LATENCY,
                "Tool calls experience 5-second delays",
                "Fetch the latest stock price for AAPL",
                new LatencyInjector(5000)));
        latencyScenarios.add(new ChaosScenario(
                "extreme-latency",
                ChaosCategory.LATENCY,
                "Tool calls experience 30-second delays",
                "Run the data analysis pipeline on the uploaded dataset",
                new LatencyInjector(30000)));
        SCENARIOS.put(ChaosCategory.LATENCY, List.copyOf(latencyScenarios));

        // Schema mutation scenarios
        List<ChaosScenario> schemaMutations = new ArrayList<>();
        schemaMutations.add(new ChaosScenario(
                "schema-envelope",
                ChaosCategory.SCHEMA_MUTATION,
                "Tool results wrapped in unexpected JSON envelope",
                "Get the current exchange rate for USD to EUR",
                new SchemaMutationInjector(
                        SchemaMutationInjector.MutationType.WRAP_IN_ENVELOPE)));
        schemaMutations.add(new ChaosScenario(
                "schema-truncated",
                ChaosCategory.SCHEMA_MUTATION,
                "Tool results are truncated mid-response",
                "List all active subscriptions for account A-9876",
                new SchemaMutationInjector(
                        SchemaMutationInjector.MutationType.TRUNCATE)));
        schemaMutations.add(new ChaosScenario(
                "schema-nested",
                ChaosCategory.SCHEMA_MUTATION,
                "Tool results nested in unexpected data structure",
                "Retrieve the shipping status for order #55443",
                new SchemaMutationInjector(
                        SchemaMutationInjector.MutationType.NEST_IN_DATA)));
        SCENARIOS.put(ChaosCategory.SCHEMA_MUTATION,
                List.copyOf(schemaMutations));

        // Cascading failure scenarios (use tool failure with multiple errors)
        List<ChaosScenario> cascading = new ArrayList<>();
        cascading.add(new ChaosScenario(
                "cascading-primary-down",
                ChaosCategory.CASCADING_FAILURE,
                "Primary service failure causing dependent tool failures",
                "Generate a sales report using data from CRM and billing",
                new ToolFailureInjector(
                        "ERROR: Upstream service unavailable "
                                + "(cascading failure from primary)")));
        SCENARIOS.put(ChaosCategory.CASCADING_FAILURE, List.copyOf(cascading));

        // Resource exhaustion scenarios
        List<ChaosScenario> resourceExhaustion = new ArrayList<>();
        resourceExhaustion.add(new ChaosScenario(
                "rate-limited",
                ChaosCategory.RESOURCE_EXHAUSTION,
                "All tools return rate limit errors",
                "Process the batch of 100 customer records",
                new ToolFailureInjector("ERROR: Rate limit exceeded")));
        SCENARIOS.put(ChaosCategory.RESOURCE_EXHAUSTION,
                List.copyOf(resourceExhaustion));
    }

    private ChaosScenarioLibrary() {}

    /**
     * Returns pre-built scenarios for the specified category.
     *
     * @param category the chaos category
     * @return list of scenarios (empty if none defined for the category)
     */
    public static List<ChaosScenario> getScenarios(ChaosCategory category) {
        return SCENARIOS.getOrDefault(category, List.of());
    }

    /**
     * Returns all pre-built scenarios across all categories.
     */
    public static List<ChaosScenario> getAllScenarios() {
        return SCENARIOS.values().stream()
                .flatMap(List::stream)
                .toList();
    }
}
