package com.agenteval.datasets.version;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Version metadata for a golden dataset.
 *
 * @param versionLabel the human-readable version label (e.g., "v1.0", "2024-03-baseline")
 * @param gitMetadata  git repository state at version time (may be null)
 * @param createdAt    when this version was created
 * @param extra        arbitrary additional metadata
 */
public record DatasetVersion(
        String versionLabel,
        GitMetadata gitMetadata,
        Instant createdAt,
        Map<String, Object> extra
) {

    public DatasetVersion {
        Objects.requireNonNull(versionLabel, "versionLabel must not be null");
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        extra = extra == null ? Map.of() : Map.copyOf(extra);
    }
}
