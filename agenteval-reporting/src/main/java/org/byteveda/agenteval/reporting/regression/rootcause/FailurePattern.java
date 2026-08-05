package org.byteveda.agenteval.reporting.regression.rootcause;

import java.util.Objects;

/**
 * A detected pattern contributing to regression failures.
 *
 * @param type the category of pattern
 * @param description human-readable description of the pattern
 * @param magnitude absolute magnitude of the change (higher = more significant)
 */
public record FailurePattern(
        PatternType type,
        String description,
        double magnitude
) {
    public FailurePattern {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
