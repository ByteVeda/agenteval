package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.judge.JudgeModel;
import org.byteveda.agenteval.core.judge.JudgeResponse;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Entry point for running chaos engineering evaluations against an agent.
 *
 * <pre>{@code
 * var result = ChaosSuite.builder()
 *     .agent(input -> myAgent.respond(input))
 *     .judgeModel(myJudge)
 *     .categories(ChaosCategory.TOOL_FAILURE, ChaosCategory.CONTEXT_CORRUPTION)
 *     .build()
 *     .run();
 * }</pre>
 */
public final class ChaosSuite {

    private static final Logger LOG = LoggerFactory.getLogger(ChaosSuite.class);
    private static final double RESILIENCE_THRESHOLD = 0.7;

    private final Function<String, String> agent;
    private final ResilienceEvaluator evaluator;
    private final Set<ChaosCategory> categories;

    private ChaosSuite(Builder builder) {
        this.agent = Objects.requireNonNull(builder.agent,
                "agent must not be null");
        this.evaluator = new ResilienceEvaluator(
                Objects.requireNonNull(builder.judgeModel,
                        "judgeModel must not be null"));
        this.categories = builder.categories;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs the chaos engineering evaluation suite.
     */
    public ChaosResult run() {
        LOG.info("Starting chaos suite with {} categories", categories.size());

        List<ChaosResult.ScenarioResult> results = new ArrayList<>();

        for (ChaosCategory category : categories) {
            List<ChaosScenario> scenarios =
                    ChaosScenarioLibrary.getScenarios(category);
            if (scenarios.isEmpty()) {
                LOG.warn("No chaos scenarios found for category: {}",
                        category);
                continue;
            }

            for (ChaosScenario scenario : scenarios) {
                try {
                    // Create a base test case and inject chaos
                    AgentTestCase baseCase = AgentTestCase.builder()
                            .input(scenario.taskInput())
                            .build();
                    AgentTestCase chaosCase = scenario.injector().inject(baseCase);

                    // Call the agent with the scenario input
                    String response = agent.apply(chaosCase.getInput());

                    // Evaluate resilience using the judge
                    JudgeResponse judgeResult = evaluator.evaluate(
                            scenario, chaosCase.getInput(), response);

                    boolean resilient =
                            judgeResult.score() >= RESILIENCE_THRESHOLD;
                    results.add(new ChaosResult.ScenarioResult(
                            category, scenario.name(),
                            chaosCase.getInput(), response,
                            judgeResult.score(), judgeResult.reason(),
                            resilient));

                    LOG.debug("Scenario [{}] score={} resilient={}",
                            scenario.name(), judgeResult.score(), resilient);
                } catch (Exception e) {
                    LOG.error("Scenario execution failed for {}: {}",
                            scenario.name(), e.getMessage());
                    results.add(new ChaosResult.ScenarioResult(
                            category, scenario.name(),
                            scenario.taskInput(),
                            "ERROR: " + e.getMessage(),
                            0.0,
                            "Agent threw exception: " + e.getMessage(),
                            false));
                }
            }
        }

        return buildResult(results);
    }

    private ChaosResult buildResult(
            List<ChaosResult.ScenarioResult> results) {
        int total = results.size();
        int resilient = (int) results.stream()
                .filter(ChaosResult.ScenarioResult::resilient).count();

        Map<ChaosCategory, List<Double>> categoryScoresList =
                new EnumMap<>(ChaosCategory.class);
        for (var result : results) {
            categoryScoresList
                    .computeIfAbsent(result.category(), k -> new ArrayList<>())
                    .add(result.score());
        }

        Map<ChaosCategory, Double> categoryScores =
                new EnumMap<>(ChaosCategory.class);
        categoryScoresList.forEach((cat, scores) ->
                categoryScores.put(cat,
                        scores.stream()
                                .mapToDouble(Double::doubleValue)
                                .average()
                                .orElse(0.0)));

        double overall = results.stream()
                .mapToDouble(ChaosResult.ScenarioResult::score)
                .average()
                .orElse(1.0);

        LOG.info("Chaos suite complete: {}/{} scenarios resilient "
                + "(score: {})", resilient, total, overall);

        return new ChaosResult(overall, categoryScores, results,
                total, resilient);
    }

    public static final class Builder {
        private Function<String, String> agent;
        private JudgeModel judgeModel;
        private Set<ChaosCategory> categories =
                Set.of(ChaosCategory.values());

        private Builder() {}

        public Builder agent(Function<String, String> agent) {
            this.agent = agent;
            return this;
        }

        public Builder judgeModel(JudgeModel judgeModel) {
            this.judgeModel = judgeModel;
            return this;
        }

        public Builder categories(ChaosCategory... categories) {
            this.categories = Set.copyOf(Arrays.asList(categories));
            return this;
        }

        public Builder categories(Set<ChaosCategory> categories) {
            this.categories = Set.copyOf(categories);
            return this;
        }

        public ChaosSuite build() {
            return new ChaosSuite(this);
        }
    }
}
