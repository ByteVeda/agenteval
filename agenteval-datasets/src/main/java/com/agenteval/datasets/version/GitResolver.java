package com.agenteval.datasets.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Resolves Git metadata from the working directory using {@code git} CLI commands.
 *
 * <p>All operations gracefully return null when git is unavailable or the directory
 * is not a git repository.</p>
 */
public final class GitResolver {

    private static final Logger LOG = LoggerFactory.getLogger(GitResolver.class);
    private static final int TIMEOUT_SECONDS = 5;

    private final Path workingDir;

    public GitResolver(Path workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * Checks whether the working directory is inside a git repository.
     */
    public boolean isGitRepository() {
        String result = runGit("rev-parse", "--is-inside-work-tree");
        return "true".equals(result);
    }

    /**
     * Resolves full git metadata from the current HEAD.
     *
     * @return the metadata, or null if git is unavailable or this is not a repository
     */
    public GitMetadata resolve() {
        if (!isGitRepository()) {
            LOG.debug("Not a git repository: {}", workingDir);
            return null;
        }

        String commitHash = runGit("rev-parse", "HEAD");
        if (commitHash == null) return null;

        String shortHash = runGit("rev-parse", "--short", "HEAD");
        String branch = runGit("rev-parse", "--abbrev-ref", "HEAD");
        String tag = runGit("describe", "--tags", "--abbrev=0");
        String timestampStr = runGit("log", "-1", "--format=%ct");
        String authorName = runGit("log", "-1", "--format=%an");
        String authorEmail = runGit("log", "-1", "--format=%ae");

        Instant timestamp = null;
        if (timestampStr != null) {
            try {
                timestamp = Instant.ofEpochSecond(Long.parseLong(timestampStr));
            } catch (NumberFormatException e) {
                LOG.debug("Could not parse git timestamp: {}", timestampStr);
            }
        }

        // "HEAD" means detached HEAD
        if ("HEAD".equals(branch)) {
            branch = null;
        }

        return new GitMetadata(commitHash, shortHash, branch, tag,
                timestamp, authorName, authorEmail);
    }

    @SuppressWarnings("IllegalCatch")
    private String runGit(String... args) {
        try {
            String[] command = new String[args.length + 1];
            command[0] = "git";
            System.arraycopy(args, 0, command, 1, args.length);

            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8).trim();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOG.debug("Git command timed out: git {}", String.join(" ", args));
                return null;
            }

            if (process.exitValue() != 0) {
                LOG.debug("Git command failed (exit {}): git {}",
                        process.exitValue(), String.join(" ", args));
                return null;
            }

            return output.isEmpty() ? null : output;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.debug("Git command error: {}", e.getMessage());
            return null;
        }
    }
}
