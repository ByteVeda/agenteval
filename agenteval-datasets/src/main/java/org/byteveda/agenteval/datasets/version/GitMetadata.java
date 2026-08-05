package org.byteveda.agenteval.datasets.version;

import java.time.Instant;

/**
 * Git repository metadata captured at the time of dataset versioning.
 *
 * @param commitHash  full commit SHA
 * @param shortHash   abbreviated commit SHA
 * @param branch      current branch name (may be null for detached HEAD)
 * @param tag         most recent tag reachable from HEAD (may be null)
 * @param timestamp   commit timestamp
 * @param authorName  commit author name
 * @param authorEmail commit author email
 */
public record GitMetadata(
        String commitHash,
        String shortHash,
        String branch,
        String tag,
        Instant timestamp,
        String authorName,
        String authorEmail
) {}
