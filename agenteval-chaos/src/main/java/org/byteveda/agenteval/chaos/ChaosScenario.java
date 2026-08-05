package org.byteveda.agenteval.chaos;

import java.util.Objects;

/**
 * Represents a chaos engineering test scenario.
 *
 * @param name short name identifying the scenario
 * @param category the chaos category this scenario belongs to
 * @param description human-readable description of the failure being simulated
 * @param taskInput the input/task to send to the agent under test
 * @param injector the chaos injector to apply before evaluation
 */
public record ChaosScenario(
        String name,
        ChaosCategory category,
        String description,
        String taskInput,
        ChaosInjector injector
) {
    public ChaosScenario {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(taskInput, "taskInput must not be null");
        Objects.requireNonNull(injector, "injector must not be null");
    }
}
