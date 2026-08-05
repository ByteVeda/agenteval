package org.byteveda.agenteval.chaos;

import org.byteveda.agenteval.core.model.AgentTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Corrupts retrieval context in a test case to simulate context-related failures.
 *
 * <p>Supports three corruption modes:</p>
 * <ul>
 *   <li>{@link CorruptionMode#MISSING} - removes context entries</li>
 *   <li>{@link CorruptionMode#CONTRADICTORY} - adds contradictory entries</li>
 *   <li>{@link CorruptionMode#SHUFFLED} - shuffles context order</li>
 * </ul>
 */
public final class ContextCorruptionInjector implements ChaosInjector {

    private final CorruptionMode mode;
    private final transient Random random;

    /**
     * Corruption modes for context manipulation.
     */
    public enum CorruptionMode {
        /** Removes all retrieval context entries. */
        MISSING,
        /** Adds contradictory information to existing context. */
        CONTRADICTORY,
        /** Shuffles the order of context entries. */
        SHUFFLED
    }

    /**
     * Creates an injector with the specified corruption mode.
     *
     * @param mode the corruption mode
     */
    public ContextCorruptionInjector(CorruptionMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.random = new Random();
    }

    /**
     * Creates an injector with the specified corruption mode and random seed.
     *
     * @param mode the corruption mode
     * @param seed the random seed for reproducible results
     */
    public ContextCorruptionInjector(CorruptionMode mode, long seed) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.random = new Random(seed);
    }

    @Override
    public AgentTestCase inject(AgentTestCase testCase) {
        Objects.requireNonNull(testCase, "testCase must not be null");
        List<String> context = testCase.getRetrievalContext();

        List<String> corrupted = switch (mode) {
            case MISSING -> List.of();
            case CONTRADICTORY -> addContradictions(context);
            case SHUFFLED -> shuffleContext(context);
        };

        return testCase.toBuilder()
                .retrievalContext(corrupted)
                .build();
    }

    @Override
    public String description() {
        return "Corrupts retrieval context using mode: " + mode;
    }

    /**
     * Returns the corruption mode used by this injector.
     */
    public CorruptionMode getMode() {
        return mode;
    }

    private List<String> addContradictions(List<String> context) {
        List<String> result = new ArrayList<>(context);
        for (String entry : context) {
            result.add("CONTRADICTORY: The opposite is true. " + entry
                    + " is actually incorrect and misleading.");
        }
        return List.copyOf(result);
    }

    private List<String> shuffleContext(List<String> context) {
        if (context.size() <= 1) {
            return context;
        }
        List<String> shuffled = new ArrayList<>(context);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled);
    }
}
