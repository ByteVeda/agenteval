package org.byteveda.agenteval.mutation;

import org.byteveda.agenteval.core.metric.EvalMetric;
import org.byteveda.agenteval.core.model.AgentTestCase;
import org.byteveda.agenteval.core.model.EvalScore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutationSuiteTest {

    @Test
    void detectsMutationWhenMetricFails() {
        EvalMetric failingMetric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.3, 0.7, "Score below threshold");
            }

            @Override
            public String name() {
                return "AlwaysFails";
            }
        };

        MutationSuiteResult result = MutationSuite.builder()
                .systemPrompt("You must always be helpful.")
                .agentFactory(prompt -> input -> "response")
                .addMutator(new WeakenConstraintMutator())
                .addMetric(failingMetric)
                .addTestInput("Hello")
                .build()
                .run();

        assertEquals(1, result.totalMutations());
        assertEquals(1, result.detectedCount());
        assertEquals(1.0, result.detectionRate());
        assertTrue(result.undetectedMutations().isEmpty());
    }

    @Test
    void reportsUndetectedMutationWhenMetricPasses() {
        EvalMetric passingMetric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.9, 0.7, "Score above threshold");
            }

            @Override
            public String name() {
                return "AlwaysPasses";
            }
        };

        MutationSuiteResult result = MutationSuite.builder()
                .systemPrompt("You must always be helpful.")
                .agentFactory(prompt -> input -> "response")
                .addMutator(new WeakenConstraintMutator())
                .addMetric(passingMetric)
                .addTestInput("Hello")
                .build()
                .run();

        assertEquals(1, result.totalMutations());
        assertEquals(0, result.detectedCount());
        assertEquals(0.0, result.detectionRate());
        assertFalse(result.undetectedMutations().isEmpty());
    }

    @Test
    void handlesMultipleMutators() {
        EvalMetric metric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.5, 0.7, "Moderate score");
            }

            @Override
            public String name() {
                return "Moderate";
            }
        };

        MutationSuiteResult result = MutationSuite.builder()
                .systemPrompt("- You must always be helpful.\n- Never be harmful.")
                .agentFactory(prompt -> input -> "response")
                .addMutator(new WeakenConstraintMutator())
                .addMutator(new RemoveInstructionMutator())
                .addMutator(new InjectContradictionMutator())
                .addMetric(metric)
                .addTestInput("Hello")
                .build()
                .run();

        assertEquals(3, result.totalMutations());
        assertEquals(3, result.detectedCount());
    }

    @Test
    void throwsWhenNoMutators() {
        assertThrows(IllegalArgumentException.class, () ->
                MutationSuite.builder()
                        .systemPrompt("prompt")
                        .agentFactory(prompt -> input -> "response")
                        .addMetric(passingMetric("Stub"))
                        .addTestInput("Hello")
                        .build()
        );
    }

    @Test
    void throwsWhenNoMetrics() {
        assertThrows(IllegalArgumentException.class, () ->
                MutationSuite.builder()
                        .systemPrompt("prompt")
                        .agentFactory(prompt -> input -> "response")
                        .addMutator(new WeakenConstraintMutator())
                        .addTestInput("Hello")
                        .build()
        );
    }

    @Test
    void throwsWhenNoTestInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                MutationSuite.builder()
                        .systemPrompt("prompt")
                        .agentFactory(prompt -> input -> "response")
                        .addMutator(new WeakenConstraintMutator())
                        .addMetric(passingMetric("Stub"))
                        .build()
        );
    }

    @Test
    void throwsWhenSystemPromptMissing() {
        assertThrows(NullPointerException.class, () ->
                MutationSuite.builder()
                        .agentFactory(prompt -> input -> "response")
                        .addMutator(new WeakenConstraintMutator())
                        .addMetric(passingMetric("Stub"))
                        .addTestInput("Hello")
                        .build()
        );
    }

    private static EvalMetric passingMetric(String metricName) {
        return new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.pass("ok");
            }

            @Override
            public String name() {
                return metricName;
            }
        };
    }

    @Test
    void pluggableMutatorIntegrates() {
        PluggableMutator custom = new PluggableMutator(
                "UpperCase",
                String::toUpperCase
        );

        EvalMetric metric = new EvalMetric() {
            @Override
            public EvalScore evaluate(AgentTestCase testCase) {
                return EvalScore.of(0.4, 0.5, "Below threshold");
            }

            @Override
            public String name() {
                return "TestMetric";
            }
        };

        MutationSuiteResult result = MutationSuite.builder()
                .systemPrompt("be helpful")
                .agentFactory(prompt -> input -> "response")
                .addMutator(custom)
                .addMetric(metric)
                .addTestInput("Hello")
                .build()
                .run();

        assertEquals(1, result.totalMutations());
        assertEquals("UpperCase", result.results().get(0).mutatorName());
        assertTrue(result.results().get(0).detected());
    }
}
