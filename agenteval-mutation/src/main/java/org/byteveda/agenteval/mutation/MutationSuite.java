package org.byteveda.agenteval.mutation;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Orchestrates mutation testing of an agent's system prompt.
 *
 * <p>Applies each configured {@link Mutator} to the system prompt, runs the agent
 * with the mutated prompt, evaluates the output, and reports which mutations
 * were detected (caused score drops).</p>
 *
 * <pre>{@code
 * var result = MutationSuite.builder()
 *     .systemPrompt("You are a helpful assistant...")
 *     .agentFactory(prompt -> input -> myAgent.call(prompt, input))
 *     .addMutator(new WeakenConstraintMutator())
 *     .addMutator(new RemoveInstructionMutator())
 *     .addMetric(new AnswerRelevancy(0.7))
 *     .addTestInput("What is the capital of France?")
 *     .build()
 *     .run();
 * }</pre>
 */
public final class MutationSuite {

    private static final Logger LOG = LoggerFactory.getLogger(MutationSuite.class);

    private final String systemPrompt;
    private final AgentFactory agentFactory;
    private final List<Mutator> mutators;
    private final List<EvalMetric> metrics;
    private final List<String> testInputs;

    private MutationSuite(Builder builder) {
        this.systemPrompt = builder.systemPrompt;
        this.agentFactory = builder.agentFactory;
        this.mutators = List.copyOf(builder.mutators);
        this.metrics = List.copyOf(builder.metrics);
        this.testInputs = List.copyOf(builder.testInputs);
    }

    /**
     * Creates a new builder.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Runs the mutation suite and returns aggregated results.
     *
     * @return the suite result containing all mutation outcomes
     */
    public MutationSuiteResult run() {
        LOG.info("Starting mutation suite: {} mutators, {} metrics, {} test inputs",
                mutators.size(), metrics.size(), testInputs.size());
        long startTime = System.currentTimeMillis();

        List<MutationResult> results = new ArrayList<>();
        for (Mutator mutator : mutators) {
            MutationResult result = runMutator(mutator);
            results.add(result);
            LOG.info("Mutator '{}': {}", mutator.name(),
                    result.detected() ? "DETECTED" : "UNDETECTED");
        }

        long durationMs = System.currentTimeMillis() - startTime;
        LOG.info("Mutation suite complete in {}ms: {}/{} detected",
                durationMs,
                results.stream().filter(MutationResult::detected).count(),
                results.size());

        return new MutationSuiteResult(results, durationMs);
    }

    private MutationResult runMutator(Mutator mutator) {
        String mutatedPrompt = mutator.mutate(systemPrompt);
        Function<String, String> agent = agentFactory.create(mutatedPrompt);

        List<EvalScore> allScores = new ArrayList<>();
        boolean detected = false;

        for (String input : testInputs) {
            String output = agent.apply(input);
            AgentTestCase testCase = AgentTestCase.builder()
                    .input(input)
                    .actualOutput(output)
                    .build();

            for (EvalMetric metric : metrics) {
                EvalScore score = metric.evaluate(testCase);
                score = score.withMetricName(metric.name());
                allScores.add(score);
                if (!score.passed()) {
                    detected = true;
                }
            }
        }

        return new MutationResult(
                mutator.name(),
                systemPrompt,
                mutatedPrompt,
                allScores,
                detected
        );
    }

    /**
     * Builder for {@link MutationSuite}.
     */
    public static final class Builder {

        private String systemPrompt;
        private AgentFactory agentFactory;
        private final List<Mutator> mutators = new ArrayList<>();
        private final List<EvalMetric> metrics = new ArrayList<>();
        private final List<String> testInputs = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the original system prompt to mutate.
         */
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * Sets the agent factory used to create agent instances.
         */
        public Builder agentFactory(AgentFactory agentFactory) {
            this.agentFactory = agentFactory;
            return this;
        }

        /**
         * Adds a mutator to the suite.
         */
        public Builder addMutator(Mutator mutator) {
            this.mutators.add(Objects.requireNonNull(mutator, "mutator must not be null"));
            return this;
        }

        /**
         * Adds all built-in mutators to the suite.
         */
        public Builder addAllBuiltInMutators() {
            this.mutators.add(new RemoveInstructionMutator());
            this.mutators.add(new WeakenConstraintMutator());
            this.mutators.add(new SwapToolDescriptionMutator());
            this.mutators.add(new InjectContradictionMutator());
            this.mutators.add(new RemoveSafetyInstructionMutator());
            return this;
        }

        /**
         * Adds an evaluation metric.
         */
        public Builder addMetric(EvalMetric metric) {
            this.metrics.add(Objects.requireNonNull(metric, "metric must not be null"));
            return this;
        }

        /**
         * Adds a test input to evaluate the mutated agent against.
         */
        public Builder addTestInput(String input) {
            this.testInputs.add(Objects.requireNonNull(input, "input must not be null"));
            return this;
        }

        /**
         * Builds the mutation suite.
         *
         * @return a new {@link MutationSuite}
         * @throws NullPointerException     if required fields are missing
         * @throws IllegalArgumentException if mutators, metrics, or inputs are empty
         */
        public MutationSuite build() {
            Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
            Objects.requireNonNull(agentFactory, "agentFactory must not be null");
            if (mutators.isEmpty()) {
                throw new IllegalArgumentException("at least one mutator is required");
            }
            if (metrics.isEmpty()) {
                throw new IllegalArgumentException("at least one metric is required");
            }
            if (testInputs.isEmpty()) {
                throw new IllegalArgumentException("at least one test input is required");
            }
            return new MutationSuite(this);
        }
    }
}
